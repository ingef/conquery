package com.bakdata.conquery.models.datasets.concepts;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.*;

@Getter
@Setter
public class MatchingStats {

    private Map<WorkerId, Entry> entries = new HashMap<>();
    @JsonIgnore
    private transient CDateRange span;

    public synchronized long countEvents() {
        return entries.values().stream().mapToLong(Entry::getNumberOfEvents).sum();
    }


    public synchronized long countEntities() {

        return entries.values().stream().mapToLong(Entry::getNumberOfEntities).sum();
    }

    public synchronized CDateRange spanEvents() {
        if (span == null) {
            span = entries.values().stream().map(Entry::getSpan).reduce(CDateRange.all(), CDateRange::spanClosed);
        }
        return span;
    }

    public void updateEntry(WorkerId source, Entry entry) {
        entries.put(source, entry);
        span = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private long numberOfEvents;

        @JsonIgnore
        private final IntSet foundEntities = new IntOpenHashSet();
        private long numberOfEntities;
        private CDateRange span;


        public void addEvent(Table table, Bucket bucket, int event, int entityForEvent) {
            numberOfEvents++;
            if(foundEntities.add(entityForEvent))
            {
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
                span = time.spanClosed(span);
            }
        }
    }

}
