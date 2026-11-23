package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.bulk.BulkRequest;
import dasein.sem1pks.dto.request.listing.ListingFilter;
import dasein.sem1pks.dto.request.listing.ListingInput;
import dasein.sem1pks.dto.request.listing.UpdateListing;
import dasein.sem1pks.dto.request.listing.Version;
import dasein.sem1pks.dto.request.note.NoteInput;
import dasein.sem1pks.dto.response.bulk.BulkResult;
import dasein.sem1pks.dto.response.common.Page;
import dasein.sem1pks.dto.response.listing.ListingView;
import dasein.sem1pks.dto.response.note.NoteView;
import dasein.sem1pks.service.BulkService;
import dasein.sem1pks.service.CatalogService;
import dasein.sem1pks.service.NoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Validated
public class CatalogController {
    private final CatalogService service;
    private final BulkService bulk;
    private final NoteService notes;

    @GetMapping
    public Page<ListingView> list(@Valid @ModelAttribute ListingFilter filter) {
        return service.search(filter);
    }

    @GetMapping("/{id}")
    public ListingView get(@PathVariable UUID id) {
        return service.visible(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingView create(
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody ListingInput data) {
        return service.create(user.id(), data);
    }

    @PutMapping("/{id}")
    public ListingView update(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody UpdateListing data) {
        return service.update(user.id(), id, data);
    }

    @DeleteMapping("/{id}")
    public ListingView archive(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @RequestParam @PositiveOrZero long version) {
        return service.archive(user.id(), id, version, true);
    }

    @PostMapping("/{id}/restore")
    public ListingView restore(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody Version data) {
        return service.archive(user.id(), id, data.version(), false);
    }

    @PostMapping("/{id}/close")
    public ListingView close(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody Version data) {
        return service.close(user.id(), id, data.version());
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @RequestParam @PositiveOrZero long version) {
        service.delete(user.id(), id, version);
    }

    @PostMapping("/bulk")
    public BulkResult bulk(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody BulkRequest data) {
        return bulk.apply(user.id(), data);
    }

    @GetMapping("/{id}/notes")
    public List<NoteView> notes(@PathVariable UUID id) {
        return notes.list(id);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteView note(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody NoteInput data) {
        return notes.create(user.id(), id, data);
    }

    @PutMapping("/{id}/notes/{noteId}")
    public NoteView updateNote(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @PathVariable UUID noteId, @Valid @RequestBody NoteInput data) {
        return notes.update(user.id(), id, noteId, data);
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(
            @AuthenticationPrincipal CurrentUser user, @PathVariable UUID id, @PathVariable UUID noteId) {
        notes.delete(user.id(), id, noteId);
    }
}
