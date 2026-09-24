package com.bakdata.conquery.models.datasets.concepts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.common.daterange.CDateRange;
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

	private Map<String, Entry> entries = new HashMap<>();
	@JsonIgnore
	private CDateRange span;

	@JsonIgnore
	private long numberOfEvents = -1L;

	@JsonIgnore
	private long numberOfEntities = -1L;

	public static MatchingStats singleEntry(String source, Entry entry) {
		MatchingStats matchingStats = new MatchingStats();
		matchingStats.entries = Map.of(source, entry);
		return matchingStats;
	}

	public synchronized long countEvents() {
		if (numberOfEvents == -1L) {
			numberOfEvents = entries.values().stream().mapToLong(Entry::getNumberOfEvents).sum();
		}
		return numberOfEvents;
	}


	public synchronized long countEntities() {
		if (numberOfEntities == -1L) {
			numberOfEntities = entries.values().stream().mapToLong(Entry::getNumberOfEntities).sum();
		}
		return numberOfEntities;
	}

	public synchronized CDateRange spanEvents() {
		if (span == null) {
			span = entries.values().stream().map(Entry::getSpan).reduce(CDateRange.all(), CDateRange::spanClosed);
		}
		return span;

	}

	public synchronized void putEntry(String source, Entry entry) {
		entries.put(source, entry);
		span = null;
		numberOfEntities = -1L;
		numberOfEvents = -1L;
	}


	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Entry {
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

	}

	/**
	 * Mutable state used only while matching statistics are being calculated.
	 *
	 * <p>The potentially large set of entity ids deliberately does not live in {@link Entry}, because entries are attached to
	 * concept elements and retained for the lifetime of the loaded dataset.</p>
	 */
	public static class Accumulator {
		private Set<String> foundEntities = new HashSet<>();
		private long numberOfEvents;
		private long numberOfEntities;
		private int minDate = Integer.MAX_VALUE;
		private int maxDate = Integer.MIN_VALUE;

		public void addEvents(String entityForEvent, int events, CDateRange time) {
			addEvents(
					entityForEvent,
					events,
					time != null && time.hasLowerBound() ? time.getMinValue() : Integer.MAX_VALUE,
					time != null && time.hasUpperBound() ? time.getMaxValue() : Integer.MIN_VALUE
			);
		}

		public void addEvents(String entityForEvent, int events, int eventMinDate, int eventMaxDate) {
			if (foundEntities == null) {
				throw new IllegalStateException("Matching stats accumulator was already finished");
			}

			numberOfEvents += events;
			if (foundEntities.add(entityForEvent)) {
				numberOfEntities++;
			}

			if (eventMaxDate != Integer.MIN_VALUE) {
				maxDate = Math.max(eventMaxDate, maxDate);
			}

			if (eventMinDate != Integer.MAX_VALUE) {
				minDate = Math.min(eventMinDate, minDate);
			}
		}

		public Entry finish() {
			Entry entry = new Entry(numberOfEvents, numberOfEntities, minDate, maxDate);
			foundEntities = null;
			return entry;
		}
	}

}
