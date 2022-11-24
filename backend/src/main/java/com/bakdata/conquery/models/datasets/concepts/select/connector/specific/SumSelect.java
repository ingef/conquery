package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.events.MajorTypeId;
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

	@NsIdRefCollection
	@NotNull
	private List<Column> distinctByColumn = Collections.emptyList();

	@NsIdRef
	@NotNull
	private Column column;

	@NsIdRef
	private Column subtractColumn;

	public SumSelect(Column column) {
		this(column, null);
	}

	public SumSelect(Column column, Column subtractColumn) {
		this.column = column;
		this.subtractColumn = subtractColumn;
	}

	@Override
	public Aggregator<? extends Number> createAggregator() {
		if (distinctByColumn != null && !distinctByColumn.isEmpty()) {
			return new DistinctValuesWrapperAggregator<>(getAggregator(), getDistinctByColumn());
		}
		return getAggregator();
	}

	private ColumnAggregator<? extends Number> getAggregator() {
		if (subtractColumn == null) {
			return switch (getColumn().getType()) {
				case INTEGER -> new IntegerSumAggregator(getColumn());
				case MONEY -> new MoneySumAggregator(getColumn());
				case DECIMAL -> new DecimalSumAggregator(getColumn());
				case REAL -> new RealSumAggregator(getColumn());
				default -> throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
			};
		}
		if (getColumn().getType() != getSubtractColumn().getType()) {
			throw new IllegalStateException(String.format("Column types are not the same: Column %s\tSubstractColumn %s", getColumn().getType(), getSubtractColumn()
					.getType()));
		}

		return switch (getColumn().getType()) {
			case INTEGER -> new IntegerDiffSumAggregator(getColumn(), getSubtractColumn());
			case MONEY -> new MoneyDiffSumAggregator(getColumn(), getSubtractColumn());
			case DECIMAL -> new DecimalDiffSumAggregator(getColumn(), getSubtractColumn());
			case REAL -> new RealDiffSumAggregator(getColumn(), getSubtractColumn());
			default -> throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
		};
	}


	private static final EnumSet<MajorTypeId> NUMBER_COMPATIBLE = EnumSet.of(MajorTypeId.INTEGER, MajorTypeId.MONEY, MajorTypeId.DECIMAL, MajorTypeId.REAL);

	@Override
	public List<Column> getRequiredColumns() {
		final List<Column> out = new ArrayList<>();

		out.add(getColumn());

		if (getSubtractColumn() != null) {
			out.add(getSubtractColumn());
		}

		if (distinctByColumn == null) {
			out.addAll(getDistinctByColumn());
		}

		return out;
	}


	@ValidationMethod(message = "Column is not of Summable Type.")
	@JsonIgnore
	public boolean isSummableColumnType() {
		return NUMBER_COMPATIBLE.contains(getColumn().getType());
	}

	@ValidationMethod(message = "Columns are not of same Type.")
	@JsonIgnore
	public boolean isColumnsOfSameType() {
		return getSubtractColumn() == null || getSubtractColumn().getType().equals(getColumn().getType());
	}
}
