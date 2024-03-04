package com.bakdata.conquery.mode.cluster;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
public class WorkerMatchingStats implements MatchingStats {

	private Map<WorkerId, Entry> entries = new HashMap<>();

	@JsonIgnore
	private transient CDateRange span;

	@JsonIgnore
	private transient long numberOfEvents = -1L;

	@JsonIgnore
	private transient long numberOfEntities = -1L;

	@Override
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


	@Override
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

	@Override
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
		private final IntSet foundEntities = new IntOpenHashSet();
		private long numberOfEntities;
		private CDateRange span;


		public void addEvent(Table table, Bucket bucket, int event, int entityForEvent) {
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
				span = time.spanClosed(span);
			}
		}
	}

}
