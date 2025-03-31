package org.faang.note.repo.redis;

import org.faang.note.models.Note;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRedisRepository extends CrudRepository<Note, Long> {
}
