package org.faang.note.controller;

import lombok.RequiredArgsConstructor;
import org.faang.note.models.Note;
import org.faang.note.models.dto.NoteDto;
import org.faang.note.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;


    @PostMapping("/grammar")
    public ResponseEntity<List<String>> checkGrammar(@RequestBody Map<String, String> payload)
            throws IOException {
        String content = payload.get("content");
        List<String> grammarIssues = noteService.checkGrammar(content);
        return ResponseEntity.ok(grammarIssues);
    }


    @PostMapping
    public ResponseEntity<Note> createNote(@RequestBody NoteDto noteDto) {
        Note note = noteService.createNote(noteDto);
        return new ResponseEntity<>(note, HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<List<Note>> listNotes() {
        return ResponseEntity.ok(noteService.getAllNotes());
    }

    @GetMapping("/{id}/render")
    public ResponseEntity<String> render(@PathVariable Long id) {
        return ResponseEntity.ok(noteService.renderHtmlForExam(id));
    }

}
