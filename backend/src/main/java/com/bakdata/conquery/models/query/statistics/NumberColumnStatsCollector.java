package com.bakdata.conquery.models.query.statistics;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import c10n.C10N;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.collect.Range;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;

@Getter
@Slf4j
public class NumberColumnStatsCollector<TYPE extends Number & Comparable<TYPE>> extends ColumnStatsCollector {

	private final ResultType type;
	private final DescriptiveStatistics statistics = new DescriptiveStatistics();
	private int nulls = 0;


	private final Comparator<TYPE> comparator;

	private final NumberFormat formatter;
	private final int expectedBins;
	private final double upperPercentile;
	private final double lowerPercentile;

	public NumberColumnStatsCollector(String name, String label, String description, ResultType type, PrintSettings printSettings, int expectedBins, double lowerPercentile, double upperPercentile) {
		super(name, label, description, printSettings);

		this.type = type;

		comparator = selectComparator(type);

		// We have to clone, as NumberFormat is not thread-safe and shared.
		formatter = selectFormatter(type, printSettings);

		this.expectedBins = expectedBins;
		this.upperPercentile = upperPercentile;
		this.lowerPercentile = lowerPercentile;
	}

	private NumberFormat selectFormatter(ResultType type, PrintSettings printSettings) {
		if (ResultType.Primitive.MONEY.equals(getType())) {
			return ((DecimalFormat) printSettings.getCurrencyFormat().clone());
		}
		else if (ResultType.Primitive.INTEGER.equals(getType())) {
			return ((NumberFormat) printSettings.getIntegerFormat().clone());
		}
		else {
			return ((NumberFormat) printSettings.getDecimalFormat().clone());
		}
	}

	private Comparator<TYPE> selectComparator(ResultType resultType) {
		// The java type system was not made to handle the silliness, sorry.
		if (ResultType.Primitive.INTEGER.equals(getType())) {
			return Comparator.comparingInt(Number::intValue);
		}

		if (ResultType.Primitive.NUMERIC.equals(getType())) {
			return Comparator.comparingDouble(Number::doubleValue);
		}

		if (ResultType.Primitive.MONEY.equals(getType())) {
			return Comparator.comparingDouble(Number::doubleValue);
		}

		throw new IllegalArgumentException("Cannot handle result type %s".formatted(resultType.toString()));
	}

	/**
	 * If distance between bounds is less than expectedBins, we expand our bounds along percentiles.
	 */
	private static Range<Double> expandBounds(double lower, double upper, int expectedBins, DescriptiveStatistics statistics, double by) {
		assert by > 0;

		// limitation of DescriptiveStatistics#getPercentile: crashes if lower==0, so we short circuit.
		final boolean underflow = lower <= 1.d;
		final boolean overflow = upper >= 99;

		final double min = underflow ? statistics.getMin() : statistics.getPercentile(lower);
		final double max = overflow ? statistics.getMax() : statistics.getPercentile(upper);

		// No need to walk further, if we are already at the limits.
		if (underflow && overflow) {
			return Range.closed(min, max);
		}

		if (max - min < expectedBins) {
			return expandBounds(Math.max(0, lower - by), Math.min(100, upper + by), expectedBins, statistics, by);
		}

		return Range.closed(min, max);
	}

	@Override
	public void consume(Object value) {
		if (value == null) {
			nulls++;
			return;
		}

		Number number = (Number) value;

		// TODO this feels like a pretty borked abstraction
		if (ResultType.Primitive.MONEY.equals(getType())) {
			number = ResultType.Primitive.MONEY.readIntermediateValue(getPrintSettings(), number);
		}

		statistics.addValue(number.doubleValue());

	}

	@Override
	public ResultColumnStatistics describe() {
		// If no real samples were collected, we short-circuit, as Statistics will throw an exception when empty.
		if (getStatistics().getN() == 0) {
			return new HistogramColumnDescription(getName(), getLabel(), getDescription(), Collections.emptyList(), Collections.emptyMap());
		}

		final List<HistogramColumnDescription.Entry> bins = createBins();
		final Map<String, String> extras = getExtras();

		return new HistogramColumnDescription(getName(), getLabel(), getDescription(), bins, extras);

	}

	@NotNull
	private List<HistogramColumnDescription.Entry> createBins() {

		final Range<Double> bounds = expandBounds(lowerPercentile, upperPercentile, expectedBins, statistics, 5);

		log.trace("Creating Histogram for {} with params inner=({},  {}), bounds=({},{}) bins={}", getLabel(), bounds.lowerEndpoint(), bounds.upperEndpoint(), getStatistics().getMin(), getStatistics().getMax(), expectedBins);

		final Histogram histogram =
				Histogram.zeroCentered(bounds.lowerEndpoint(), bounds.upperEndpoint(), getStatistics().getMin(), getStatistics().getMax(), expectedBins, bounds.upperEndpoint() - bounds.lowerEndpoint() > 1);

		Arrays.stream(getStatistics().getValues()).forEach(histogram::add);

		return histogram.nodes()
						.stream()
						.map(bin -> {
							final String binLabel = bin.createLabel(this::printValue, ResultType.Primitive.INTEGER.equals(getType()));

							return new HistogramColumnDescription.Entry(binLabel, bin.getCount());
						})
						.toList();
	}

	@NotNull
	private Map<String, String> getExtras() {
		final StatisticsLabels labels = C10N.get(StatisticsLabels.class, getPrintSettings().getLocale());


		// LinkedHashMap remembers insertion order
		final LinkedHashMap<String, String> out = new LinkedHashMap<>();

		out.put(labels.min(), printValue(getStatistics().getMin()));
		out.put(labels.max(), printValue(getStatistics().getMax()));

		// mean is always a decimal number, therefore integer needs special handling
		if(ResultType.Primitive.INTEGER.equals(getType())){
			out.put(labels.mean(), getPrintSettings().getDecimalFormat().format(getStatistics().getMean()));
		}
		else {
			out.put(labels.mean(), printValue(getStatistics().getMean()));
		}

		out.put(labels.p25(), printValue(getStatistics().getPercentile(25)));
		out.put(labels.median(), printValue(getStatistics().getPercentile(50)));
		out.put(labels.p75(), printValue(getStatistics().getPercentile(75)));

		out.put(labels.std(), getPrintSettings().getDecimalFormat().format(getStatistics().getStandardDeviation()));

		out.put(labels.sum(), printValue(getStatistics().getSum()));
		out.put(labels.count(), getPrintSettings().getIntegerFormat().format(getStatistics().getN()));
		out.put(labels.missing(), getPrintSettings().getIntegerFormat().format(getNulls()));

		return out;
	}


	private String printValue(Number value) {
		return formatter.format(value.doubleValue());
	}


}
