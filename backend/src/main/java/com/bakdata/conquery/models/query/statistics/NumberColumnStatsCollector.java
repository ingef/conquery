package com.bakdata.conquery.models.query.statistics;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.DoubleStream;

import c10n.C10N;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.dynatrace.dynahist.Histogram;
import com.dynatrace.dynahist.bin.Bin;
import com.dynatrace.dynahist.layout.CustomLayout;
import com.dynatrace.dynahist.layout.Layout;
import com.dynatrace.dynahist.layout.LogLinearLayout;
import lombok.Getter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;

@Getter
public class NumberColumnStatsCollector<TYPE extends Number & Comparable<TYPE>> extends ColumnStatsCollector<Number> {
	private final DescriptiveStatistics statistics = new DescriptiveStatistics();
	private final AtomicLong nulls = new AtomicLong(0);


	private final Comparator<TYPE> comparator;

	private final NumberFormat formatter;

	public NumberColumnStatsCollector(String name, String label, String description, ResultType type, PrintSettings printSettings) {
		super(name, label, description, type, printSettings);
		comparator = selectComparator(type);

		if (type instanceof ResultType.MoneyT) {
			formatter = DecimalFormat.getCurrencyInstance(I18n.LOCALE.get());
			formatter.setCurrency(printSettings.getCurrency());
			formatter.setMaximumFractionDigits(printSettings.getCurrency().getDefaultFractionDigits());
		}
		else if (type instanceof ResultType.IntegerT) {
			formatter = printSettings.getIntegerFormat();
		}
		else {
			formatter = printSettings.getDecimalFormat();
		}
	}

	private Comparator<TYPE> selectComparator(ResultType resultType) {
		// The java type system was not made to handle the silliness, sorry.
		if (resultType instanceof ResultType.IntegerT) {
			return Comparator.comparingInt(Number::intValue);
		}

		if (resultType instanceof ResultType.NumericT) {
			return Comparator.comparingDouble(Number::doubleValue);
		}

		if (resultType instanceof ResultType.MoneyT) {
			return Comparator.comparingDouble(Number::doubleValue);
		}

		throw new IllegalArgumentException("Cannot handle result type %s".formatted(resultType.toString()));
	}

	@Override
	public void consume(Number value) {
		if (value == null) {
			nulls.incrementAndGet();
			return;
		}

		// TODO this feels like a pretty borked abstraction
		if (getType() instanceof ResultType.MoneyT moneyT) {
			value = moneyT.readIntermediateValue(getPrintSettings(), value);
		}

		statistics.addValue(value.doubleValue());

	}

	@Override
	public ResultColumnStatistics describe() {
		// If no real samples were collected, we short-circuit, as Statistics will throw an exception when empty.
		if (getStatistics().getN() == 0) {
			return new StringColumnStatsCollector.ColumnDescription(getName(), getLabel(), getDescription(), Collections.emptyList(), Collections.emptyMap());
		}

		final List<StringColumnStatsCollector.ColumnDescription.Entry> bins = createBins(getStatistics().getN(), 15d);
		final Map<String, String> extras = getExtras();

		return new StringColumnStatsCollector.ColumnDescription(getName(), getLabel(), getDescription(), bins, extras);
	}

	@NotNull
	private List<StringColumnStatsCollector.ColumnDescription.Entry> createBins(long total, double expectedBins) {
		final Layout layout = getLayout();

		final Histogram histogram = Histogram.createDynamic(layout);

		Arrays.stream(getStatistics().getValues()).forEach(histogram::addValue);

		final List<Count> bins = postProcessBins((double) total, expectedBins, histogram);


		final List<StringColumnStatsCollector.ColumnDescription.Entry> entries = new ArrayList<>();


		for (Count bin : bins) {
			final String lower = printValue(bin.min());
			final String upper = printValue(bin.max());

			final String binLabel = String.format("%s - %s", lower, upper);


			entries.add(new StringColumnStatsCollector.ColumnDescription.Entry(binLabel, bin.count()));
		}
		return entries;
	}

	@NotNull
	private Map<String, String> getExtras() {
		final StatisticsLabels labels = C10N.get(StatisticsLabels.class);

		// LinkedHashMap remembers insertion order
		final LinkedHashMap<String, String> out = new LinkedHashMap<>();

		out.put(labels.min(), printValue(getStatistics().getMin()));
		out.put(labels.max(), printValue(getStatistics().getMax()));
		out.put(labels.mean(), printValue(getStatistics().getMean()));

		out.put(labels.p25(), printValue(getStatistics().getPercentile(25)));
		out.put(labels.median(), printValue(getStatistics().getPercentile(50)));
		out.put(labels.p75(), printValue(getStatistics().getPercentile(75)));
		out.put(labels.std(), getPrintSettings().getDecimalFormat().format(getStatistics().getStandardDeviation()));

		out.put(labels.sum(), printValue(getStatistics().getSum()));

		out.put(labels.count(), getPrintSettings().getIntegerFormat().format(getStatistics().getN()));
		out.put(labels.missing(), getPrintSettings().getIntegerFormat().format(getNulls().get()));

		return out;
	}

	@NotNull
	private Layout getLayout() {
		if (getType() instanceof ResultType.IntegerT) {
			return getIntegerLayout();
		}

		final Layout initial = LogLinearLayout.create(getStatistics().getStandardDeviation() / 2, 1 / 10d, getStatistics().getMin(), getStatistics().getMax());


		return initial;
	}

	/**
	 * Merge small bins from left to right order
	 */
	@NotNull
	private static List<Count> postProcessBins(double total, double expectedBins, Histogram histogram) {
		final List<Count> bins = new ArrayList<>();

		Count prior = null;

		for (Bin bin : histogram.nonEmptyBinsDescending()) {
			final Count current = Count.fromBin(bin);

			if (prior == null) {
				prior = current;
				continue;
			}

			// If the bin is too small, we merge-left
			if ((double) prior.count() / total <= (1 / expectedBins)) {
				prior = prior.merge(current);
				continue;
			}

			// Only emit bin, if we cannot merge left.
			bins.add(prior);
			prior = null;
		}

		bins.add(prior);
		return bins;
	}

	private String printValue(Number value) {
		return formatter.format(value.doubleValue());
	}

	private Layout getIntegerLayout() {
		final int expectedBins = 15;

		final int min = (int) Math.round(getStatistics().getMin());
		final int max = (int) Math.round(getStatistics().getMin());

		final int nBins = Math.min(1 + min - max, expectedBins);

		final int width = (min - max) / nBins;


		return CustomLayout.create(DoubleStream.iterate(min, cur -> cur <= max, cur -> cur + width).toArray());
	}

	private record Count(double min, double max, long count) {

		public static Count fromBin(Bin bin) {
			return new Count(bin.getLowerBound(), bin.getUpperBound(), bin.getBinCount());
		}

		public Count merge(Count other) {
			return new Count(Math.min(min(), other.min()), Math.max(max(), other.max()), count() + other.count());
		}

	}

}
