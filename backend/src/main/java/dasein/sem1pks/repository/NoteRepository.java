package dasein.sem1pks.repository;

import dasein.sem1pks.dto.response.NoteView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository {

    List<NoteView> findByListing(UUID listingId);

    Optional<NoteView> find(UUID listingId, UUID noteId);

    void insert(UUID id, UUID listingId, UUID authorId, String body, boolean pinned);

    void update(UUID id, String body, boolean pinned);

    void delete(UUID id);
}
