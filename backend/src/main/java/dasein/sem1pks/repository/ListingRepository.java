package dasein.sem1pks.repository;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.ListingStatus;
import org.springframework.data.domain.Sort;


import java.util.List;
import java.util.Optional;

public interface ListingRepository {

    Listing save(Listing listing);

    Optional<Listing> findById(Long id);

    List<Listing> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Listing> search(
            String prefix,
            ListingCategory category,
            ListingStatus closedStatus,
            ListingStatus soldStatus,
            Sort sort
    );
}
