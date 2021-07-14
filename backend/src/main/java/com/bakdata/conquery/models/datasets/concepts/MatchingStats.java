package com.bakdata.conquery.models.datasets.concepts;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingStats {

	private Map<WorkerId, Entry> entries = new HashMap<>();
	@JsonIgnore
	private transient CDateRange span;
	@JsonIgnore
	private transient long numberOfEvents = -1;

	public synchronized long countEvents() {
		if (numberOfEvents == -1L) {
			numberOfEvents = entries.values().stream().mapToLong(Entry::getNumberOfEvents).sum();
		}
		return numberOfEvents;
	}

	public synchronized CDateRange spanEvents() {
		if (span == null) {
			span = entries.values().stream().map(Entry::getSpan).reduce(CDateRange.all(), CDateRange::spanClosed);
		}
		return span;
	}

	public void updateEntry(WorkerId source, Entry entry) {
		entries.put(source, entry);
		numberOfEvents = -1;
		span = null;
	}

	@Data
	public static class Entry {
		private long numberOfEvents = 0;
		private CDateRange span;

		public void addEvent(Table table, Bucket bucket, int event) {
			numberOfEvents++;
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
