package dasein.sem1pks.service;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.User;
import dasein.sem1pks.dto.response.ListingResponse;
import dasein.sem1pks.exception.notfound.AccountNotFoundException;
import dasein.sem1pks.repository.ListingRepository;
import dasein.sem1pks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingResponse createListing(
            Long id,
            String title,
            String description,
            BigDecimal price,
            ListingCategory category
    ) {

        User user = userRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));

        Listing listing = new Listing();

        listing.setDescription(description);
        listing.setTitle(title);
        listing.setUser(user);
        listing.setPrice(price);
        listing.setCategory(category != null ? category : ListingCategory.OTHER);


        return toDto(listingRepository.save(listing));

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
