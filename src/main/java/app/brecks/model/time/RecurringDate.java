package app.brecks.model.time;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.util.Iterator;
import java.util.Map;

@AllArgsConstructor
@Setter
@Getter
public class RecurringDate implements TemporalAdjuster {
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<TemporalField, Long> pattern;

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return adjust(temporal, this.pattern.entrySet().iterator());
    }

    private Temporal adjust(Temporal temporal, Iterator<Map.Entry<TemporalField, Long>> patternIterator) {
        if (patternIterator.hasNext()) {
            Map.Entry<TemporalField, Long> patternEntry = patternIterator.next();
            return adjust(temporal.with(patternEntry.getKey(), patternEntry.getValue()), patternIterator);
        } else return temporal;
    }
}