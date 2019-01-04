package com.bakdata.conquery.models.preproc.outputs.daysinrange;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
public class TypeIdDaysInRangeMerger {

	private static final Comparator<DaysInRangeEntry> COMPARATOR =
			Comparator.<DaysInRangeEntry, LocalDate>comparing(e -> e.getDaysInRange().getStart())
					.thenComparing(e -> e.getDaysInRange().getEnd());

	private final Table<String, Identifier, NavigableSet<DaysInRangeEntry>> ranges = HashBasedTable.create();
	private final Consumer<PatientEvent> consumer;
	private final Predicate<DaysInRange> mergeCondition;
	private final Function<DaysInRange, CDateRange> rangeFunction;


	private void ensureInitialized(Identifier identifier, String type) {
		if(!ranges.contains(type, identifier)) {
			ranges.put(type, identifier, new TreeSet<>(COMPARATOR));
		}
	}

	public void processAll(Collection<DaysInRangeEntry> entries) {
		entries.forEach(this::process);
	}

	/**
	 * Find already processed entries, that are immediately adjacent to current and join merge them.
	 * @param current
	 */
	public void process(DaysInRangeEntry current) {
		if(!isMergeable(current.getDaysInRange())) {
			emit(current);
			return;
		}

		Identifier identifier = current.getIdentifier();
		String type = current.getType();

		ensureInitialized(identifier, type);

		final NavigableSet<DaysInRangeEntry> matches = ranges.get(type, identifier);

		// Find nearest predecessor and merge if possible
		final DaysInRangeEntry prior = matches.floor(current);

		if(prior != null && prior.getDaysInRange().immediatelyPrecedes(current.getDaysInRange())) {
			current = prior.append(current);
			matches.remove(prior);
		}

		// Find nearest successor and merge if possible
		final DaysInRangeEntry after = matches.ceiling(current);

		if(after != null && current.getDaysInRange().immediatelyPrecedes(after.getDaysInRange())) {
			current = current.append(after);
			matches.remove(after);
		}

		if(isMergeable(current.getDaysInRange())) {
			matches.add(current);
		}
		else {
			emit(current);
		}
	}


	public void clearRemaining() {
		ranges
				.values()
				.stream()
				.flatMap(NavigableSet::stream)
				.forEach(this::emit);
	}

	private boolean isMergeable(DaysInRange daysInRange) {
		return mergeCondition.test(daysInRange);
	}


	private void emit(DaysInRangeEntry daysInRange) {
		PatientEvent patientEvent =
				new PatientEvent(
						daysInRange.getPrimaryId(),
						daysInRange.getIdentifier(),
						daysInRange.getType(),
						daysInRange.getColumns(),
						rangeFunction.apply(daysInRange.getDaysInRange())
				);

		consumer.accept(patientEvent);
	}

	@AllArgsConstructor
	@Getter
	@Data
	public static class DaysInRangeEntry {

		@NotNull
		private final int primaryId;
		@NotNull
		private final PPColumn[] columns;
		@Valid
		private final DaysInRange daysInRange;
		@NotNull
		private final Identifier identifier;
		@NotNull
		private final String type;

		/**
		 * Join two entries, and their days in range.
		 * @param entry
		 * @return
		 */
		public DaysInRangeEntry append(DaysInRangeEntry entry){
			if(!this.type.equals(entry.type) && this.identifier != entry.identifier) {
				throw new IllegalArgumentException("Not allowed to merge non matching entries");
			}

			DaysInRange days =
					new DaysInRange(CDateRange.of(this.getDaysInRange().fromEnd(), entry.getDaysInRange().fromStart()), this.daysInRange.getDays()
																														 + entry.daysInRange.getDays());
			return new DaysInRangeEntry(this.primaryId, this.columns, days, this.identifier, this.type);
		}
	}
}
