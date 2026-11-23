package dasein.sem1pks.repository;

import dasein.sem1pks.dto.request.listing.ListingFilter;
import dasein.sem1pks.dto.request.listing.ListingInput;
import dasein.sem1pks.dto.response.common.Page;
import dasein.sem1pks.dto.response.listing.ListingView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogRepository {

    Optional<ListingView> find(UUID id);

    Optional<ListingView> findCode(String code);

    Page<ListingView> search(ListingFilter filter);

    List<ListingView> all(ListingFilter filter);

    UUID insert(UUID owner, ListingInput data);

    void update(UUID id, long version, ListingInput data);

    void archive(UUID id, long version, boolean archived);

    void close(UUID id, long version);

    void delete(UUID id, long version);
}
