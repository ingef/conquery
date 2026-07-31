package com.bakdata.conquery.quarkus.concepts.selects.specific;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.CountQuartersSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.CountSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DateDistanceSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DateUnionSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DistinctSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DurationSumSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.FirstSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.FlagsSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.LastSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.MappableSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.PrefixSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.QuartersInYearSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.RandomSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.SingleColumnSelectDefinition;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.SumSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

public final class BuiltinSelectProviders {

	private BuiltinSelectProviders() {
	}

	private abstract static class SingleColumnProvider<T extends SingleColumnSelectDefinition> extends AbstractBuiltinSelectProvider<T> {
		protected SingleColumnProvider(Class<T> payloadType) {
			super(payloadType);
		}

		protected DatasetCatalogRepository.Select convertColumn(SelectConversionContext context, T payload) {
			ColumnId column = context.columnId(payload.getColumn());
			return select(context, payload, resultType(context, column), List.of(column));
		}
	}

	private abstract static class MappableProvider<T extends MappableSelectDefinition> extends SingleColumnProvider<T> {
		protected MappableProvider(Class<T> payloadType) {
			super(payloadType);
		}

		protected DatasetCatalogRepository.Select convertMapped(SelectConversionContext context, T payload) {
			ColumnId column = context.columnId(payload.getColumn());
			if (payload.getMapping() != null || payload.getSubstring() != null) requireColumnType(context, column, DatasetCatalogRepository.ColumnType.STRING);
			DatasetCatalogRepository.SelectResultType result = payload.getMapping() == null ? resultType(context, column) : primitive("STRING");
			return select(context, payload, result, List.of(column));
		}
	}

	@ApplicationScoped
	public static class DistinctProvider extends MappableProvider<DistinctSelectDefinition> {
		public DistinctProvider() { super(DistinctSelectDefinition.class); }
		@Override public String type() { return "DISTINCT"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, DistinctSelectDefinition payload) {
			ColumnId column = context.columnId(payload.getColumn());
			if (payload.getMapping() != null || payload.getSubstring() != null) requireColumnType(context, column, DatasetCatalogRepository.ColumnType.STRING);
			return select(context, payload, DatasetCatalogRepository.SelectResultType.list(payload.getMapping() == null ? resultType(context, column) : primitive("STRING")), List.of(column));
		}
	}

	@ApplicationScoped
	public static class FirstProvider extends MappableProvider<FirstSelectDefinition> {
		public FirstProvider() { super(FirstSelectDefinition.class); }
		@Override public String type() { return "FIRST"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, FirstSelectDefinition payload) { return convertMapped(context, payload); }
	}

	@ApplicationScoped
	public static class LastProvider extends MappableProvider<LastSelectDefinition> {
		public LastProvider() { super(LastSelectDefinition.class); }
		@Override public String type() { return "LAST"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, LastSelectDefinition payload) { return convertMapped(context, payload); }
	}

	@ApplicationScoped
	public static class RandomProvider extends MappableProvider<RandomSelectDefinition> {
		public RandomProvider() { super(RandomSelectDefinition.class); }
		@Override public String type() { return "RANDOM"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, RandomSelectDefinition payload) { return convertMapped(context, payload); }
	}

	@ApplicationScoped
	public static class CountProvider extends AbstractBuiltinSelectProvider<CountSelectDefinition> {
		public CountProvider() { super(CountSelectDefinition.class); }
		@Override public String type() { return "COUNT"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, CountSelectDefinition payload) {
			List<ColumnId> columns = new ArrayList<>();
			columns.add(context.columnId(payload.getColumn()));
			columns.addAll(optionalColumns(context, payload.getDistinctByColumn()));
			return select(context, payload, primitive("INTEGER"), columns);
		}
	}

	@ApplicationScoped
	public static class CountQuartersProvider extends AbstractBuiltinSelectProvider<CountQuartersSelectDefinition> {
		public CountQuartersProvider() { super(CountQuartersSelectDefinition.class); }
		@Override public String type() { return "COUNT_QUARTERS"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, CountQuartersSelectDefinition payload) {
			List<ColumnId> columns = dateRangeColumns(context, payload);
			requireDateRangeTypes(context, columns);
			return select(context, payload, primitive("INTEGER"), columns);
		}
	}

	@ApplicationScoped
	public static class DateDistanceProvider extends SingleColumnProvider<DateDistanceSelectDefinition> {
		public DateDistanceProvider() { super(DateDistanceSelectDefinition.class); }
		@Override public String type() { return "DATE_DISTANCE"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, DateDistanceSelectDefinition payload) {
			ColumnId column = context.columnId(payload.getColumn());
			requireColumnType(context, column, DatasetCatalogRepository.ColumnType.DATE, DatasetCatalogRepository.ColumnType.DATE_RANGE);
			return select(context, payload, primitive("INTEGER"), List.of(column));
		}
	}

	@ApplicationScoped
	public static class DateUnionProvider extends AbstractBuiltinSelectProvider<DateUnionSelectDefinition> {
		public DateUnionProvider() { super(DateUnionSelectDefinition.class); }
		@Override public String type() { return "DATE_UNION"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, DateUnionSelectDefinition payload) {
			List<ColumnId> columns = dateRangeColumns(context, payload);
			requireDateRangeTypes(context, columns);
			return select(context, payload, list("DATE_RANGE"), columns);
		}
	}

	@ApplicationScoped
	public static class DurationSumProvider extends AbstractBuiltinSelectProvider<DurationSumSelectDefinition> {
		public DurationSumProvider() { super(DurationSumSelectDefinition.class); }
		@Override public String type() { return "DURATION_SUM"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, DurationSumSelectDefinition payload) {
			List<ColumnId> rangeColumns = dateRangeColumns(context, payload);
			requireDateRangeTypes(context, rangeColumns);
			return select(context, payload, primitive("INTEGER"), append(rangeColumns, optionalColumns(context, payload.getDistinctBy())));
		}
	}

	@ApplicationScoped
	public static class FlagsProvider extends AbstractBuiltinSelectProvider<FlagsSelectDefinition> {
		public FlagsProvider() { super(FlagsSelectDefinition.class); }
		@Override public String type() { return "FLAGS"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, FlagsSelectDefinition payload) {
			List<ColumnId> columns = payload.getFlags().values().stream().map(context::columnId).toList();
			columns.forEach(column -> requireColumnType(context, column, DatasetCatalogRepository.ColumnType.BOOLEAN));
			return select(context, payload, list("STRING"), columns);
		}
	}

	@ApplicationScoped
	public static class PrefixProvider extends SingleColumnProvider<PrefixSelectDefinition> {
		public PrefixProvider() { super(PrefixSelectDefinition.class); }
		@Override public String type() { return "PREFIX"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, PrefixSelectDefinition payload) {
			ColumnId column = context.columnId(payload.getColumn());
			requireColumnType(context, column, DatasetCatalogRepository.ColumnType.STRING);
			return select(context, payload, list("STRING"), List.of(column));
		}
	}

	@ApplicationScoped
	public static class QuartersInYearProvider extends SingleColumnProvider<QuartersInYearSelectDefinition> {
		public QuartersInYearProvider() { super(QuartersInYearSelectDefinition.class); }
		@Override public String type() { return "QUARTERS_IN_YEAR"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, QuartersInYearSelectDefinition payload) {
			ColumnId column = context.columnId(payload.getColumn());
			requireColumnType(context, column, DatasetCatalogRepository.ColumnType.DATE, DatasetCatalogRepository.ColumnType.DATE_RANGE);
			return select(context, payload, primitive("INTEGER"), List.of(column));
		}
	}

	@ApplicationScoped
	public static class SumProvider extends SingleColumnProvider<SumSelectDefinition> {
		public SumProvider() { super(SumSelectDefinition.class); }
		@Override public String type() { return "SUM"; }
		@Override public DatasetCatalogRepository.Select convert(SelectConversionContext context, SumSelectDefinition payload) {
			ColumnId column = context.columnId(payload.getColumn());
			requireColumnType(context, column, DatasetCatalogRepository.ColumnType.INTEGER, DatasetCatalogRepository.ColumnType.MONEY, DatasetCatalogRepository.ColumnType.DECIMAL, DatasetCatalogRepository.ColumnType.REAL);
			List<ColumnId> columns = new ArrayList<>(List.of(column));
			if (payload.getSubtractColumn() != null && !payload.getSubtractColumn().isBlank()) {
				ColumnId subtractColumn = context.columnId(payload.getSubtractColumn());
				if (context.columnType(subtractColumn) != context.columnType(column)) throw new IllegalArgumentException("Select SUM columns must have the same type.");
				columns.add(subtractColumn);
			}
			columns.addAll(optionalColumns(context, payload.getDistinctByColumn()));
			return select(context, payload, resultType(context, column), columns);
		}
	}
}
