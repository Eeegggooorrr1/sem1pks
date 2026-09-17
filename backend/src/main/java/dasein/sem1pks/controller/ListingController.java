package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.dto.request.ListingCreateRequest;
import dasein.sem1pks.dto.request.ListingSortBy;
import dasein.sem1pks.dto.response.ListingResponse;
import dasein.sem1pks.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Validated
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    public ResponseEntity<ListingResponse> createListing(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ListingCreateRequest request
    ) {
        ListingResponse response = listingService.createListing(
                currentUser.id(),
                request.title(),
                request.description(),
                request.price(),
                request.category()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{listingId}/sold")
    public ResponseEntity<ListingResponse> markAsSold(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long listingId
    ) {
        return ResponseEntity.ok(listingService.markAsSold(currentUser.id(), listingId));
    }

    @PatchMapping("/{listingId}/active")
    public ResponseEntity<ListingResponse> markAsActive(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long listingId
    ) {
        return ResponseEntity.ok(listingService.markAsActive(currentUser.id(), listingId));
    }

    @PatchMapping("/{listingId}/closed")
    public ResponseEntity<Void> markAsClosed(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long listingId
    ) {
        listingService.markAsClosed(currentUser.id(), listingId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{listingId}/closed/admin")
    public ResponseEntity<Void> markAsClosedAsAdmin(
            @PathVariable Long listingId
    ) {
        listingService.markAsClosedAsAdmin(listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<ListingResponse>> findMyListings(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(
                listingService.findMyListings(currentUser.id())
        );
    }

    @GetMapping
    public ResponseEntity<List<ListingResponse>> search(
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) ListingCategory category,
            @RequestParam(defaultValue = "CREATED_AT") ListingSortBy sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        return ResponseEntity.ok(listingService.search(prefix, category, sortBy, direction));
    }
}