package dasein.sem1pks.dto.request.listing;

import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.domain.Priority;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ListingFilter {
    public enum Comparison { EQ, GT, GTE, LT, LTE }
    public enum SortBy { TITLE, PRICE, CREATED_AT, UPDATED_AT }
    public enum Direction { ASC, DESC }

    @Size(max=200) private String search;
    private UUID categoryId;
    private ListingStatus status;
    private Boolean featured;
    private Priority priority;
    @Size(max=40) private String tag;
    private Boolean archived = false;
    @DecimalMin("0") @Digits(integer=8,fraction=2) private BigDecimal price;
    @NotNull private Comparison priceOp = Comparison.EQ;
    @NotNull private SortBy sortBy = SortBy.CREATED_AT;
    @NotNull private Direction direction = Direction.DESC;
    @Min(0) @Max(1000000) private int page;
    @Min(1) @Max(100) private int size = 20;
}
