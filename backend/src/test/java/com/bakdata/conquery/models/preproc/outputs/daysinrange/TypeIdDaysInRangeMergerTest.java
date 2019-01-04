package com.bakdata.conquery.models.preproc.outputs.daysinrange;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.google.common.base.Predicates;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class TypeIdDaysInRangeMergerTest {

	/**
	 * Pass mergeable entries with gaps between. Entries should be emited only after clearing the merger.
	 */
	@Test
	public void withGapMergeable() {
		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(Predicates.alwaysTrue())
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		List<CDateRange> fullMonths = getMonthsOf(2018).stream()
																 .filter(month -> month.getMax().getMonth().getValue() % 2 == 0)
																 .collect(Collectors.toList());

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entries =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays()))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0], month, new Identifier(Lists.newArrayList(1)), "a"))
						  .collect(Collectors.toList());


		merger.processAll(entries);

		assertThat(output).isEmpty();

		merger.clearRemaining();

		assertThat(output).hasSize(fullMonths.size());
	}

	/**
	 * Pass not mergeable entries into merger, entries should be emitted immediately after processing and not after clearing.
	 */
	@Test
	public void withGapNotMergeable() {
		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(Predicates.alwaysFalse())
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		List<CDateRange> fullMonths = getMonthsOf(2018).stream()
																 .filter(month -> month.getMax().getMonth().getValue() % 2 == 0)
																 .collect(Collectors.toList());

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entries =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays()))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0], month, new Identifier(Lists.newArrayList(1)), "a"))
						  .collect(Collectors.toList());

		merger.processAll(entries);

		assertThat(output).hasSize(fullMonths.size());

		merger.clearRemaining();

		assertThat(output).hasSize(fullMonths.size());
	}

	/**
	 * Pass entries into merger, that are adjacent. Return value should be a single entry of the full time span of entries.
	 */
	@Test
	public void withoutGapMergeable() {
		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(Predicates.alwaysTrue())
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		List<CDateRange> fullMonths = getMonthsOf(2018);

		CDateRange firstMonth = fullMonths.get(0);
		CDateRange lastMonth = fullMonths.get(fullMonths.size() - 1);

//		Collections.shuffle(fullMonths);

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entries =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays() + 1))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0],month, new Identifier(Lists.newArrayList(1)), "a"))
						  .collect(Collectors.toList());


		// All entries are adjacent and of the same category, so will be merged into a single entry, of the entire year.
		merger.processAll(entries);

		assertThat(output).hasSize(0);

		merger.clearRemaining();

		assertThat(output).hasSize(1);

		log.info("{}", output.get(0));

		assertThat(output.get(0).getRange().getMin()).isEqualTo(firstMonth.getMin());
		assertThat(output.get(0).getRange().getMax()).isEqualTo(lastMonth.getMax());
	}

	/**
	 * Pass adjacent entries into merger, that fail the test for merging, these should immediately be emitted.
	 */
	@Test
	public void withoutGapNotMergeable() {
		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(Predicates.alwaysFalse())
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		List<CDateRange> fullMonths = getMonthsOf(2018);

		Collections.shuffle(fullMonths);

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entries =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays() + 1))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0],month, new Identifier(Lists.newArrayList(1)), "a"))
						  .collect(Collectors.toList());


		// All entries are adjacent and of the same category, so will be merged into a single entry, of the entire year.
		merger.processAll(entries);

		assertThat(output).hasSize(fullMonths.size());

		merger.clearRemaining();

		assertThat(output).hasSize(fullMonths.size());
	}

	/**
	 * Merge adjacent entries of two types (a,b). Result should contain two entries for each type.
	 */
	@Test
	public void withoutGapMergeable2Types() {
		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(a -> true)
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		List<CDateRange> fullMonths = getMonthsOf(2018);

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entriesA =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays() + 1))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0],month, new Identifier(Lists.newArrayList(1)), "a"))
						  .collect(Collectors.toList());

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entriesB =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays() + 1))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0],month, new Identifier(Lists.newArrayList(1)), "b"))
						  .collect(Collectors.toList());



		merger.processAll(entriesA);
		assertThat(output).hasSize(0);

		merger.processAll(entriesB);

		merger.clearRemaining();

		assertThat(output).hasSize(2);
	}

	/**
	 * Pass adjacent entries of two Ids into merger and expect them to be returned as two seperate merged entries.
	 */
	@Test
	public void withoutGapMergeable2Ids() {
		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(Predicates.alwaysTrue())
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		List<CDateRange> fullMonths = getMonthsOf(2018);

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entriesA =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays() + 1))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0],month, new Identifier(Lists.newArrayList(1)), "a"))
						  .collect(Collectors.toList());

		List<TypeIdDaysInRangeMerger.DaysInRangeEntry> entriesB =
				fullMonths.stream()
						  .map(month -> new DaysInRange(month, month.getDurationInDays() + 1))
						  .map(month -> new TypeIdDaysInRangeMerger.DaysInRangeEntry(0, new PPColumn[0] ,month, new Identifier(Lists.newArrayList(2)), "a"))
						  .collect(Collectors.toList());

		merger.processAll(entriesA);
		assertThat(output).hasSize(0);

		merger.processAll(entriesB);

		merger.clearRemaining();

		assertThat(output).hasSize(2);
	}

	/**
	 * Submit a síngle random entry and assert that it was not altered.
	 */
	@RepeatedTest(10)
	public void mergeableHasColumnsAndId() {
		Random random = new Random();

		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(Predicates.alwaysTrue())
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		int pid = random.nextInt();
		PPColumn[] columns = new PPColumn[random.nextInt(100)];
		CDateRange month = getMonthsOf(1900 + random.nextInt(500)).get(random.nextInt(12));

		merger.processAll(Lists.newArrayList(new TypeIdDaysInRangeMerger.DaysInRangeEntry(pid, columns, new DaysInRange(month, month.getDurationInDays() + 1), new Identifier(Lists.newArrayList(2)), "a")));

		merger.clearRemaining();

		PatientEvent event = output.get(0);

		assertThat(event.getPrimaryId()).isEqualTo(pid);
		assertThat(event.getColumns()).isEqualTo(columns);
	}


	/**
	 * Submit a síngle random entry and assert that it was not altered.
	 */
	@RepeatedTest(10)
	public void notMergeableHasColumnsAndId() {
		Random random = new Random();

		ArrayList<PatientEvent> output = new ArrayList<>();

		TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																.consumer(output::add)
																.mergeCondition(Predicates.alwaysFalse())
																.rangeFunction(DaysInRange::rangeFromStart)
																.build();

		int pid = random.nextInt();
		PPColumn[] columns = new PPColumn[random.nextInt(100)];
		CDateRange month = getMonthsOf(1900 + random.nextInt(500)).get(random.nextInt(12));

		merger.processAll(Lists.newArrayList(new TypeIdDaysInRangeMerger.DaysInRangeEntry(pid, columns, new DaysInRange(month, month.getDurationInDays() + 1), new Identifier(Lists.newArrayList(2)), "a")));

		merger.clearRemaining();

		PatientEvent event = output.get(0);

		assertThat(event.getPrimaryId()).isEqualTo(pid);
		assertThat(event.getColumns()).isEqualTo(columns);
	}


	private static List<CDateRange> getMonthsOf(int year) {
		return Arrays.stream(Month.values())
					 .map(month -> {
						 LocalDate first = LocalDate.of(year, month, 1).with(TemporalAdjusters.firstDayOfMonth());
						 LocalDate last = first.with(TemporalAdjusters.lastDayOfMonth());

						 return CDateRange.of(first, last);
					 })
					 .collect(Collectors.toList());
	}
}