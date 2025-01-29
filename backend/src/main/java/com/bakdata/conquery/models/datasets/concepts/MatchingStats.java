package com.bakdata.conquery.models.datasets.concepts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchingStats {

	private final Map<String, MatchingStats.Entry> entries = new HashMap<>();

	@JsonIgnore
	private CDateRange span;

	@JsonIgnore
	private long numberOfEvents = -1L;

	@JsonIgnore
	private long numberOfEntities = -1L;

	public synchronized long countEvents() {
		if (numberOfEvents == -1L) {
			numberOfEvents = entries.values().stream().mapToLong(MatchingStats.Entry::getNumberOfEvents).sum();
		}
		return numberOfEvents;
	}


	public synchronized long countEntities() {
		if (numberOfEntities == -1L) {
			numberOfEntities = entries.values().stream().mapToLong(MatchingStats.Entry::getNumberOfEntities).sum();
		}
		return numberOfEntities;
	}

	public synchronized CDateRange spanEvents() {
		if (span == null) {
			span = entries.values().stream().map(MatchingStats.Entry::getSpan).reduce(CDateRange.all(), CDateRange::spanClosed);
		}
		return span;

	}

	public synchronized void putEntry(String source, MatchingStats.Entry entry) {
		entries.put(source, entry);
		span = null;
		numberOfEntities = -1L;
		numberOfEvents = -1L;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Entry {
		@JsonIgnore
		private final Set<String> foundEntities = new HashSet<>();
		private long numberOfEvents;
		private long numberOfEntities;
		private int minDate = Integer.MAX_VALUE;
		private int maxDate = Integer.MIN_VALUE;

		@JsonIgnore
		public CDateRange getSpan() {
			if (minDate == Integer.MAX_VALUE && maxDate == Integer.MIN_VALUE) {
				return null;
			}

			return CDateRange.of(
					minDate == Integer.MAX_VALUE ? Integer.MIN_VALUE : minDate,
					maxDate == Integer.MIN_VALUE ? Integer.MAX_VALUE : maxDate
			);
		}

		public void addEventFromBucket(String entityForEvent, Bucket bucket, int event, Table table) {

			int maxDate = Integer.MIN_VALUE;
			int minDate = Integer.MAX_VALUE;


			for (Column c : table.getColumns()) {
				if (!c.getType().isDateCompatible()) {
					continue;
				}

				if (!bucket.has(event, c)) {
					continue;
				}

				final CDateRange time = bucket.getAsDateRange(event, c);

				if (time.hasUpperBound()) {
					maxDate = Math.max(time.getMaxValue(), maxDate);
				}

				if (time.hasLowerBound()) {
					minDate = Math.min(time.getMinValue(), minDate);
				}
			}

			final CDateRange span;

			if (minDate == Integer.MAX_VALUE && maxDate == Integer.MIN_VALUE) {
				span = null;
			}
			else if (minDate == Integer.MAX_VALUE) {
				span = CDateRange.atMost(maxDate);
			}
			else if (maxDate == Integer.MIN_VALUE) {
				span = CDateRange.atLeast(minDate);
			}
			else {
				span = CDateRange.of(minDate, maxDate);
			}

			addEvents(entityForEvent, 1, span);
		}

		public void addEvents(String entityForEvent, int events, CDateRange time) {
			numberOfEvents += events;
			if (foundEntities.add(entityForEvent)) {
				numberOfEntities++;
			}

			if (time == null) {
				return;
			}

			if (time.hasUpperBound()) {
				maxDate = Math.max(time.getMaxValue(), maxDate);
			}

			if (time.hasLowerBound()) {
				minDate = Math.min(time.getMinValue(), minDate);
			}
		}
	}

}
