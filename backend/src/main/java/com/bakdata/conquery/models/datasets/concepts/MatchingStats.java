package com.bakdata.conquery.models.datasets.concepts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class MatchingStats {

    private Map<WorkerId, Entry> entries = new HashMap<>();
    @JsonIgnore
    private transient CDateRange span;

    @JsonIgnore
    private transient long numberOfEvents = -1L;

    @JsonIgnore
    private transient long numberOfEntities = -1L;

    public long countEvents() {
        if (numberOfEvents == -1L) {
            synchronized (this) {
                if (numberOfEvents == -1L) {
                    numberOfEvents = entries.values().stream().mapToLong(Entry::getNumberOfEvents).sum();
                }
            }
        }
        return numberOfEvents;
    }


    public long countEntities() {
        if (numberOfEntities == -1L) {
            synchronized (this) {
                if (numberOfEntities == -1L) {
                    numberOfEntities = entries.values().stream().mapToLong(Entry::getNumberOfEntities).sum();
                }
            }
        }
        return numberOfEntities;
    }

    public CDateRange spanEvents() {
        if (span == null) {
            synchronized (this) {
                if (span == null) {
                    span = entries.values().stream().map(Entry::getSpan).reduce(CDateRange.all(), CDateRange::spanClosed);
                }
            }
        }
        return span;

    }

    public void putEntry(WorkerId source, Entry entry) {
        synchronized (this) {
            entries.put(source, entry);
            span = null;
            numberOfEntities = -1L;
            numberOfEvents = -1L;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private long numberOfEvents;

        @JsonIgnore
        private final Set<String> foundEntities = new HashSet<>();
        private long numberOfEntities;
		private int minDate = Integer.MAX_VALUE;
		private int maxDate = Integer.MIN_VALUE;

		public CDateRange getSpan() {
			if(minDate == Integer.MAX_VALUE && maxDate == Integer.MIN_VALUE) {
				return null;
			}

			return CDateRange.of(
					minDate == Integer.MAX_VALUE ? Integer.MIN_VALUE : minDate,
					maxDate == Integer.MIN_VALUE ? Integer.MAX_VALUE : maxDate
			);
		}

        public void addEvent(Table table, Bucket bucket, int event, String entityForEvent) {
            numberOfEvents++;
            if (foundEntities.add(entityForEvent)) {
                numberOfEntities++;
            }

            for (Column c : table.getColumns()) {
                if (!c.getType().isDateCompatible()) {
                    continue;
                }

                if (!bucket.has(event, c)) {
                    continue;
                }

                final CDateRange time = bucket.getAsDateRange(event, c);

				if (time.hasUpperBound()){
					maxDate = Math.max(time.getMaxValue(), maxDate);
				}

				if (time.hasLowerBound()){
					minDate = Math.min(time.getMinValue(), minDate);
				}
            }
        }
    }

}
