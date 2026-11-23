package dasein.sem1pks.service;

import dasein.sem1pks.dto.request.note.NoteInput;
import dasein.sem1pks.dto.response.note.NoteView;
import dasein.sem1pks.exception.forbidden.CatalogAccessDeniedException;
import dasein.sem1pks.exception.notfound.CatalogEntryNotFoundException;
import dasein.sem1pks.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository notes;
    private final CatalogService listings;
    private final AuditService audit;

    public List<NoteView> list(UUID listingId) {
        listings.visible(listingId);
        return notes.findByListing(listingId);
    }

    @Transactional
    public NoteView create(UUID actor, UUID listingId, NoteInput input) {
        listings.visible(listingId);

        UUID id = UUID.randomUUID();
        notes.insert(id, listingId, actor, input.body().trim(), input.pinned());
        audit.record(actor, "NOTE_CREATE", "NOTE", id, "Заметка создана");
        return get(listingId, id);
    }

    @Transactional
    public NoteView update(UUID actor, UUID listingId, UUID id, NoteInput input) {
        checkEditable(actor, get(listingId, id));

        notes.update(id, input.body().trim(), input.pinned());
        audit.record(actor, "NOTE_UPDATE", "NOTE", id, "Заметка изменена");
        return get(listingId, id);
    }

    @Transactional
    public void delete(UUID actor, UUID listingId, UUID id) {
        checkEditable(actor, get(listingId, id));

        notes.delete(id);
        audit.record(actor, "NOTE_DELETE", "NOTE", id, "Заметка удалена");
    }

    private NoteView get(UUID listingId, UUID id) {
        listings.visible(listingId);
        return notes.find(listingId, id).orElseThrow(CatalogEntryNotFoundException::new);
    }

    private void checkEditable(UUID actor, NoteView note) {
        if (!note.authorId().equals(actor) && !CatalogService.isAdmin()) {
            throw new CatalogAccessDeniedException();
        }
    }
}
