package com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregator summing over {@code addendColumn} and subtracting over {@code subtrahendColumn}, for decimal columns.
 */
@ToString(of = {"addendColumn", "subtrahendColumn"})
public class DecimalDiffSumAggregator extends ColumnAggregator<BigDecimal> {

	private boolean hit;

	@Getter
	private final Column addendColumn;
	@Getter
	private final Column subtrahendColumn;
	private BigDecimal sum = BigDecimal.ZERO;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		sum = BigDecimal.ZERO;
	}


	public DecimalDiffSumAggregator(Column addend, Column subtrahend) {
		addendColumn = addend;
		subtrahendColumn = subtrahend;
	}

	@Override
	public List<Column> getRequiredColumns() {
		final List<Column> out = new ArrayList<>();

		out.add(getAddendColumn());
		out.add(getSubtrahendColumn());

		return out;
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getAddendColumn()) && !bucket.has(event, getSubtrahendColumn())) {
			return;
		}

		hit = true;

		BigDecimal addend = bucket.has(event, getAddendColumn()) ? bucket.getDecimal(event, getAddendColumn()) : BigDecimal.ZERO;

		BigDecimal subtrahend = bucket.has(event, getSubtrahendColumn()) ? bucket.getDecimal(event, getSubtrahendColumn()) : BigDecimal.ZERO;

		sum = sum.add(addend.subtract(subtrahend));
	}

	@Override
	public BigDecimal createAggregationResult() {
		return hit ? sum : null;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NumericT.INSTANCE;
	}

}
