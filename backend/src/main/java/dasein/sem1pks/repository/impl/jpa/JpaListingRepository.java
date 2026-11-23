package dasein.sem1pks.repository.impl.jpa;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.repository.ListingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaListingRepository
        extends JpaRepository<Listing, UUID>, ListingRepository {
    @Override
    default Listing save(Listing listing) {
        return saveAndFlush(listing);
    }


    @Override
    Optional<Listing> findById(UUID id);

    @Override
    @Query(value = "SELECT id FROM listings WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<UUID> lockById(@Param("id") UUID id);

    @Override
    List<Listing> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Override
    @Query("""
            select listing
            from Listing listing
            where listing.status <> :closedStatus
              and (
                  cast(:prefix as String) is null
                  or lower(listing.title) like lower(concat(cast(:prefix as String), '%')) escape '!'
              )
              and (cast(:categoryId as uuid) is null or listing.categoryId = :categoryId)
            order by case
                when listing.status = :soldStatus then 1
                else 0
            end asc
            """)
    List<Listing> search(
            @Param("prefix") String prefix,
            @Param("categoryId") UUID categoryId,
            @Param("closedStatus") ListingStatus closedStatus,
            @Param("soldStatus") ListingStatus soldStatus,
            Sort sort
    );
}
