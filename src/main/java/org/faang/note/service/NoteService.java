package org.faang.note.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.faang.note.models.Note;
import org.faang.note.models.dto.NoteDto;
import org.faang.note.repo.jpa.NoteRepository;
import org.languagetool.JLanguageTool;
import org.languagetool.language.English;
import org.languagetool.rules.RuleMatch;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository repository;
    private final RedisTemplate<String , Object> redisTemplate;

    private static final String NOTE_CACHE_PREFIX = "note:";
    private static final String NOTE_CACHE_SUFFIX = "note:html";
    private static final String ALL_NOTES_CACHE_KEY = "all_notes";

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public List<String> checkGrammarNote(long id) throws IOException {
        Note note = repository.findById(id).orElse(null);
        log.info("Checking grammar note with id " + id);
        if (note == null) {
            return List.of();
        }

        JLanguageTool tool = new JLanguageTool(new English());
        List<RuleMatch> matches = tool.check(note.getContent());
        log.info("Checking grammar note with id " + id);

        return matches.stream()
                .map(match -> "Line " + match.getLine() + ", column " + match.getColumn() + ": " + match.getMessage())
                .toList();

    }

    public List<String> checkGrammar(String text) throws IOException {
        log.info("Checking grammar");
        JLanguageTool tool = new JLanguageTool(new English());
        long startTime = System.currentTimeMillis();
        List<RuleMatch> matches = tool.check(text);
        log.info("Finished checking grammar");
        return matches.stream()
                .map(match -> "Line " + match.getLine() + ", column " + match.getColumn() + ": " + match.getMessage())
                .toList();
    }

    public Note createNote(NoteDto note) {
        log.info("Creating note: " + note);
        if (note.getTitle() == null || note.getTitle().isEmpty()) {
            throw new IllegalArgumentException("Note title cannot be null or empty");
        }

        log.info("Note title: " + note.getTitle());
        Note noteEntity = new Note();
        noteEntity.setTitle(note.getTitle());
        noteEntity.setContent(note.getContent());
        Note save = repository.save(noteEntity);
        log.info("Note created: " + save);

        redisTemplate.opsForValue().set(NOTE_CACHE_PREFIX + save.getId(), save, 10, TimeUnit.MINUTES);
        redisTemplate.delete(NOTE_CACHE_SUFFIX + save.getId()); // invalidate HTML cache
        log.info("Note created: " + save);
        return save;
    }

    public List<Note> getAllNotes() {
        List<Note> cachedNotes = (List<Note>) redisTemplate.opsForValue().get(ALL_NOTES_CACHE_KEY);
        if (cachedNotes != null && !cachedNotes.isEmpty()) {
            log.info("All notes: " + cachedNotes);
            return cachedNotes;
        }

        log.info("All notes found");
        List<Note> notes = repository.findAll();
        if (notes != null && !notes.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(ALL_NOTES_CACHE_KEY, notes.toArray());
            log.info("All notes found");
        }
        log.info("All notes found");
        return notes;
    }

    public Optional<Note> getNoteById(Long id) {
        log.info("Get note by id: " + id);
        String key = NOTE_CACHE_PREFIX + id;
        Note cached = (Note) redisTemplate.opsForValue().get(key);
        log.info("Get note by id: " + id);
        if (cached != null) {
            return Optional.of(cached);
        }

        log.info("Get note by id: " + id);
        Optional<Note> note = repository.findById(id);
        log.info("Get note by id: " + id);
        note.ifPresent(n -> redisTemplate.opsForValue().set(key, n, 10, TimeUnit.MINUTES));
        return note;
    }

    public String renderHtml(Long id) {
        String key = NOTE_CACHE_PREFIX + id;
        log.info("Get note by id: " + id);
        String cachedHtml = (String) redisTemplate.opsForValue().get(key);
        if (cachedHtml != null) {
            return cachedHtml;
        }
        log.info("Get note by id: " + id);

        Note note = repository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        String html = renderer.render(parser.parse(note.getContent()));
        log.info("Get note by id: " + id);
        redisTemplate.opsForValue().set(key, html, 10, TimeUnit.MINUTES);
        log.info("Get note by id: " + id);
        return html;
    }

    public String renderHtmlForExam(long id) {
        String htmlKey = NOTE_CACHE_SUFFIX + id; // ✅ Properly separate cache key for HTML
        log.info("Get note HTML by id: " + id);

        String cachedHtml = (String) redisTemplate.opsForValue().get(htmlKey);
        if (cachedHtml != null) {
            return cachedHtml;
        }

        Note note = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        String html = renderer.render(parser.parse(note.getContent()));

        redisTemplate.opsForValue().set(htmlKey, html, 10, TimeUnit.MINUTES); // ✅
        // Store with the right key
        return html;
    }

}
