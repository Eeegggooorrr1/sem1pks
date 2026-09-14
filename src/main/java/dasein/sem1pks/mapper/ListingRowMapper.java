package dasein.sem1pks.mapper;

import dasein.sem1pks.domain.Listing;
import dasein.sem1pks.domain.ListingCategory;
import dasein.sem1pks.domain.ListingStatus;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
public class ListingRowMapper {

    public Listing map(ResultSet resultSet) throws SQLException {
        Listing listing = new Listing();

        listing.setId(resultSet.getLong("id"));
        listing.setTitle(resultSet.getString("title"));
        listing.setDescription(resultSet.getString("description"));
        listing.setPrice(resultSet.getBigDecimal("price"));
        listing.setStatus(
                ListingStatus.valueOf(resultSet.getString("status"))
        );
        listing.setCategory(
                ListingCategory.valueOf(resultSet.getString("category"))
        );

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            listing.setCreatedAt(createdAt.toLocalDateTime());
        }

        return listing;
    }
}
