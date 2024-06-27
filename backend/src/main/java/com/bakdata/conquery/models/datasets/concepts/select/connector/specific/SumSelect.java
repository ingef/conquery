package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.DecimalDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.IntegerDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.MoneyDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.RealDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.DecimalSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.IntegerSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.MoneySumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.RealSumAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "SUM", base = Select.class)
@NoArgsConstructor(onConstructor_ = @JsonCreator)
public class SumSelect extends Select {

	@NotNull
	private List<ColumnId> distinctByColumn = Collections.emptyList();

	@NotNull
	private ColumnId column;

	private ColumnId subtractColumn;

	public SumSelect(ColumnId column) {
		this(column, null);
	}

	public SumSelect(ColumnId column, ColumnId subtractColumn) {
		this.column = column;
		this.subtractColumn = subtractColumn;
	}

	@Override
	public Aggregator<? extends Number> createAggregator() {
		if (distinctByColumn != null && !distinctByColumn.isEmpty()) {
			return new DistinctValuesWrapperAggregator<>(getAggregator(), getDistinctByColumn().stream().map(ColumnId::resolve).toList());
		}
		return getAggregator();
	}

	private ColumnAggregator<? extends Number> getAggregator() {
		final Column resolved = getColumn().resolve();
		if (subtractColumn == null) {
			return switch (resolved.getType()) {
				case INTEGER -> new IntegerSumAggregator(resolved);
				case MONEY -> new MoneySumAggregator(resolved);
				case DECIMAL -> new DecimalSumAggregator(resolved);
				case REAL -> new RealSumAggregator(resolved);
				default -> throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", resolved.getType()));
			};
		}
		final Column subtrahend = getSubtractColumn().resolve();
		if (resolved.getType() != subtrahend.getType()) {
			throw new IllegalStateException(String.format("Column types are not the same: Column %s\tSubstractColumn %s", resolved.getType(), subtrahend
					.getType()));
		}

		return switch (resolved.getType()) {
			case INTEGER -> new IntegerDiffSumAggregator(resolved, subtrahend);
			case MONEY -> new MoneyDiffSumAggregator(resolved, subtrahend);
			case DECIMAL -> new DecimalDiffSumAggregator(resolved, subtrahend);
			case REAL -> new RealDiffSumAggregator(resolved, subtrahend);
			default -> throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", resolved.getType()));
		};
	}


	private static final EnumSet<MajorTypeId> NUMBER_COMPATIBLE = EnumSet.of(MajorTypeId.INTEGER, MajorTypeId.MONEY, MajorTypeId.DECIMAL, MajorTypeId.REAL);

	@Override
	public List<ColumnId> getRequiredColumns() {
		final List<ColumnId> out = new ArrayList<>();

		out.add(getColumn());

		if (getSubtractColumn() != null) {
			out.add(getSubtractColumn());
		}

		if (distinctByColumn == null) {
			out.addAll(getDistinctByColumn());
		}

		return out;
	}

	@Override
	public ResultType<?> getResultType() {
		return ResultType.resolveResultType(getColumn().resolve().getType());
	}

	@ValidationMethod(message = "Column is not of Summable Type.")
	@JsonIgnore
	public boolean isSummableColumnType() {
		return NUMBER_COMPATIBLE.contains(getColumn().resolve().getType());
	}

	@ValidationMethod(message = "Columns are not of same Type.")
	@JsonIgnore
	public boolean isColumnsOfSameType() {
		return getSubtractColumn() == null || getSubtractColumn().resolve().getType().equals(getColumn().resolve().getType());
	}

	@Override
	public SelectConverter<SumSelect> createConverter() {
		return new SumSqlAggregator<>();
	}
}
