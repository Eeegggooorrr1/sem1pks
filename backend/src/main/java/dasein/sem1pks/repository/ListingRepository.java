package dasein.sem1pks.repository;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingStatus;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository {

    Listing save(Listing listing);

    Optional<Listing> findById(UUID id);

    Optional<UUID> lockById(UUID id);

    List<Listing> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Listing> search(
            String prefix,
            UUID categoryId,
            ListingStatus closedStatus,
            ListingStatus soldStatus,
            Sort sort
    );
}
