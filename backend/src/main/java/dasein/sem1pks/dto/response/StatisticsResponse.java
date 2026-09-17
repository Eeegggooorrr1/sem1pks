package dasein.sem1pks.dto.response;

public record StatisticsResponse(
        long usersCount,
        long ordersCount,
        long listingsCount
) {
}