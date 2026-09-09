package dasein.sem1pks.repository;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.ListingStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select listing from Listing listing
            where listing.status <> :closedStatus
              and (:prefix is null or lower(listing.title) like lower(concat(:prefix, '%')))
              and (:category is null or listing.category = :category)
            order by case when listing.status = :soldStatus then 1 else 0 end asc
            """)
    List<Listing> search(
            @Param("prefix") String prefix,
            @Param("category") ListingCategory category,
            @Param("closedStatus") ListingStatus closedStatus,
            @Param("soldStatus") ListingStatus soldStatus,
            Sort sort
    );
}
