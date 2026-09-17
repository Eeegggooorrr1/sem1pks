package dasein.sem1pks.mapper;

import dasein.sem1pks.domain.Order;
import dasein.sem1pks.domain.OrderStatus;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


@Component
public class OrderRowMapper {

    public Order map(ResultSet resultSet) throws SQLException {
        Order order = new Order();

        order.setId(resultSet.getLong("id"));
        order.setVersion(resultSet.getLong("version"));

        order.setStatus(
                OrderStatus.valueOf(resultSet.getString("status"))
        );

        Timestamp orderDate = resultSet.getTimestamp("order_date");
        if (orderDate != null) {
            order.setOrderDate(orderDate.toLocalDateTime());
        }

        dasein.sem1pks.domain.User buyer = new dasein.sem1pks.domain.User();
        buyer.setId(resultSet.getLong("buyer_id"));
        dasein.sem1pks.domain.User seller = new dasein.sem1pks.domain.User();
        seller.setId(resultSet.getLong("seller_id"));
        dasein.sem1pks.domain.Listing listing = new dasein.sem1pks.domain.Listing();
        listing.setId(resultSet.getLong("listing_id"));
        listing.setUser(seller);
        order.setBuyer(buyer);
        order.setListing(listing);
        return order;
    }
}
