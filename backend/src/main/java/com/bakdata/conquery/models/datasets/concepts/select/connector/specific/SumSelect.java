package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.EnumSet;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
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

	@Getter
	@Setter
	@NsIdRef
	private Column distinctByColumn;

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
		if (distinctByColumn != null) {
			return new DistinctValuesWrapperAggregator<>(getAggregator(), getDistinctByColumn());
		}
		return getAggregator();
	}

	private ColumnAggregator<? extends Number> getAggregator() {
		if (subtractColumn == null) {
			switch (getColumn().getType()) {
				case INTEGER:
					return new IntegerSumAggregator(getColumn());
				case MONEY:
					return new MoneySumAggregator(getColumn());
				case DECIMAL:
					return new DecimalSumAggregator(getColumn());
				case REAL:
					return new RealSumAggregator(getColumn());
				default:
					throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
			}
		}
		if (getColumn().getType() != getSubtractColumn().getType()) {
			throw new IllegalStateException(String.format("Column types are not the same: Column %s\tSubstractColumn %s", getColumn().getType(), getSubtractColumn()
																																						 .getType()));
		}
		switch (getColumn().getType()) {
			case INTEGER:
				return new IntegerDiffSumAggregator(getColumn(), getSubtractColumn());
			case MONEY:
				return new MoneyDiffSumAggregator(getColumn(), getSubtractColumn());
			case DECIMAL:
				return new DecimalDiffSumAggregator(getColumn(), getSubtractColumn());
			case REAL:
				return new RealDiffSumAggregator(getColumn(), getSubtractColumn());
			default:
				throw new IllegalStateException(String.format("Invalid column type '%s' for SUM Aggregator", getColumn().getType()));
		}
	}


	private static final EnumSet<MajorTypeId> NUMBER_COMPATIBLE = EnumSet.of(MajorTypeId.INTEGER, MajorTypeId.MONEY, MajorTypeId.DECIMAL, MajorTypeId.REAL);


	@ValidationMethod(message = "Column is not of Summable Type.")
	@JsonIgnore
	public boolean isSummableColumnType() {
		return  NUMBER_COMPATIBLE.contains(getColumn().getType());
	}

	@ValidationMethod(message = "Columns are not of same Type.")
	@JsonIgnore
	public boolean isColumnsOfSameType() {
		return getSubtractColumn() == null || getSubtractColumn().getType().equals(getColumn().getType());
	}
}
