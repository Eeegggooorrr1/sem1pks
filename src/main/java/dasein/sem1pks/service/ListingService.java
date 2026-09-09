package dasein.sem1pks.service;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.domain.User;
import dasein.sem1pks.dto.request.ListingSortBy;
import dasein.sem1pks.dto.response.ListingResponse;
import dasein.sem1pks.exception.conflict.ListingStateConflictException;
import dasein.sem1pks.exception.forbidden.ListingAccessDeniedException;
import dasein.sem1pks.exception.notfound.AccountNotFoundException;
import dasein.sem1pks.exception.notfound.ListingNotFoundException;
import dasein.sem1pks.repository.ListingRepository;
import dasein.sem1pks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public ListingResponse createListing(
            Long userId,
            String title,
            String description,
            BigDecimal price,
            ListingCategory category
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        Listing listing = new Listing();

        listing.setDescription(description);
        listing.setTitle(title);
        listing.setUser(user);
        listing.setPrice(price);
        listing.setCategory(category != null ? category : ListingCategory.OTHER);

        return toDto(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse markAsSold(Long userId, Long listingId) {
        Listing listing = getListing(listingId);

        if (!listing.getUser().getId().equals(userId)) {
            throw new ListingAccessDeniedException(listingId);
        }
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ListingAccessDeniedException(listingId);
        }
        listing.setStatus(ListingStatus.SOLD);

        return toDto(listing);
    }

    @Transactional
    public ListingResponse markAsActive(Long userId, Long listingId) {
        Listing listing = getListing(listingId);

        if (!listing.getUser().getId().equals(userId)) {
            throw new ListingAccessDeniedException(listingId);
        }
        if (listing.getStatus() != ListingStatus.SOLD) {
            throw new ListingStateConflictException();
        }
        listing.setStatus(ListingStatus.ACTIVE);

        return toDto(listing);
    }

    @Transactional
    public void markAsClosed(Long userId, Long listingId) {
        Listing listing = getListing(listingId);


        if (!listing.getUser().getId().equals(userId)) {
            throw new ListingAccessDeniedException(listingId);
        }
        listing.setStatus(ListingStatus.CLOSED);
    }

    @Transactional
    public void markAsClosedAsAdmin(Long listingId) {
        Listing listing = getListing(listingId);
        listing.setStatus(ListingStatus.CLOSED);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> findMyListings(Long userId) {
        return listingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> search(
            String prefix,
            ListingCategory category,
            ListingSortBy sortBy,
            Sort.Direction direction
    ) {
        Sort sort = Sort.by(direction, toProperty(sortBy))
                .and(Sort.by(Sort.Direction.DESC, "id"));

        return listingRepository.search(
                        normalizePrefix(prefix),
                        category,
                        ListingStatus.CLOSED,
                        ListingStatus.SOLD,
                        sort
                ).stream()
                .map(this::toDto)
                .toList();
    }


    private Listing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return null;
        }
        return prefix.trim();
    }

    private String toProperty(ListingSortBy sortBy) {
        return switch (sortBy) {
            case PRICE -> "price";
            case CREATED_AT -> "createdAt";
        };
    }

    private ListingResponse toDto(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPrice(),
                listing.getStatus(),
                listing.getCategory(),
                listing.getUser().getId(),
                listing.getCreatedAt()
        );
    }
}
