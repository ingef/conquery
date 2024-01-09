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

import c10n.C10N;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
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

		final List<StringColumnStatsCollector.ColumnDescription.Entry> bins = createBins(15);
		final Map<String, String> extras = getExtras();

		return new StringColumnStatsCollector.ColumnDescription(getName(), getLabel(), getDescription(), bins, extras);
	}

	@NotNull
	private List<StringColumnStatsCollector.ColumnDescription.Entry> createBins(int expectedBins) {

		final double min = getStatistics().getPercentile(20d);
		final double max = getStatistics().getPercentile(80d);

		final BalancingHistogram histogram = BalancingHistogram.create(min, max, expectedBins);

		Arrays.stream(getStatistics().getValues()).forEach(histogram::add);

		final List<BalancingHistogram.Node> balanced = histogram.nodes();


		final List<StringColumnStatsCollector.ColumnDescription.Entry> entries = new ArrayList<>();


		for (BalancingHistogram.Node bin : balanced) {
			final String lower = printValue(bin.getMin());
			final String upper = printValue(bin.getMax());

			final String binLabel = lower.equals(upper) ? lower : String.format("%s - %s", lower, upper);


			entries.add(new StringColumnStatsCollector.ColumnDescription.Entry(binLabel, bin.getCount()));
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


	private String printValue(Number value) {
		return formatter.format(value.doubleValue());
	}


}
