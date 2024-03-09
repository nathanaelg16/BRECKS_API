package app.brecks.model.time;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Interval {
    @NonNull
    private final LocalDate startDate;

    @NonNull
    private final LocalDate endDate;

    public static Interval between(@NonNull LocalDate startDateInclusive, @NonNull LocalDate endDateInclusive) {
        return new Interval(startDateInclusive, endDateInclusive);
    }

    public boolean contains(LocalDate date) {
        return (this.startDate.isBefore(date) || this.startDate.isEqual(date)) &&
                (this.endDate.isAfter(date) || this.endDate.isEqual(date));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Interval interval = (Interval) o;

        return new EqualsBuilder().append(startDate, interval.startDate).append(endDate, interval.endDate).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(startDate).append(endDate).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("startDate", startDate)
                .append("endDate", endDate)
                .toString();
    }
}
