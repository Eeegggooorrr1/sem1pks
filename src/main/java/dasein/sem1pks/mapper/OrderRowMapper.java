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

        order.setStatus(
                OrderStatus.valueOf(resultSet.getString("status"))
        );

        Timestamp orderDate = resultSet.getTimestamp("order_date");
        if (orderDate != null) {
            order.setOrderDate(orderDate.toLocalDateTime());
        }

        return order;
    }
}
