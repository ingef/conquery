package com.bakdata.conquery.models.datasets.concepts;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.*;
import org.bouncycastle.util.Strings;

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

	public synchronized void addEntry(String source, Entry entry) {
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
		private static final HashFunction hash = Hashing.fingerprint2011();

		private LongSet foundEntities = new LongLinkedOpenHashSet();
		private long numberOfEvents;
		private long numberOfEntities;
		private int minDate = Integer.MAX_VALUE;
		private int maxDate = Integer.MIN_VALUE;

		public static long hashEntity(String entity){
			return hash.hashBytes(Strings.toByteArray(entity)).asLong();
		}

		public void addEvents(long hashedEntity, int nEvents, CDateRange span) {
			addEvents(
					hashedEntity, nEvents,
					span != null && span.hasLowerBound() ? span.getMinValue() : Integer.MAX_VALUE,
					span != null && span.hasUpperBound() ? span.getMaxValue() : Integer.MIN_VALUE
			);
		}

		public void addEvents(long hashedEntity, int nEvents, int eventMinDate, int eventMaxDate) {
			if (foundEntities == null) {
				throw new IllegalStateException("Matching stats accumulator was already finished");
			}

			numberOfEvents += nEvents;

			// This is not perfectly distinct but has low memory and CPU footprint which is overall more important.
			if (foundEntities.add(hashedEntity)) {
				numberOfEntities++;
			}

			if (eventMaxDate != Integer.MIN_VALUE) {
				maxDate = Math.max(eventMaxDate, maxDate);
			}

			if (eventMinDate != Integer.MAX_VALUE) {
				minDate = Math.min(eventMinDate, minDate);
			}
		}

		public Entry toEntry() {
			Entry entry = new Entry(numberOfEvents, numberOfEntities, minDate, maxDate);
			foundEntities = null;
			return entry;
		}
	}

}
