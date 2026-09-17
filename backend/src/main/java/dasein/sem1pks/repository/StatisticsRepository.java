package dasein.sem1pks.repository;


public interface StatisticsRepository {

    StatisticsProjection getStatistics();

    interface StatisticsProjection {

        long getUsersCount();

        long getOrdersCount();

        long getListingsCount();
    }
}