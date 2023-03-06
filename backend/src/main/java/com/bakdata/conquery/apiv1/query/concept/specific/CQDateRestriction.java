package com.bakdata.conquery.apiv1.query.concept.specific;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.DateRestrictingNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@CPSType(id = "DATE_RESTRICTION", base = CQElement.class)
@Getter
@RequiredArgsConstructor
public class CQDateRestriction extends CQElement {
	@NotNull
	@Valid
	private final Range<LocalDate> dateRange;
	@Valid
	@NotNull
	private final CQElement child;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		QPNode childQueryPlan = child.createQueryPlan(context, plan);

		return new DateRestrictingNode(
				CDateSet.create(Collections.singleton(CDateRange.of(dateRange))),
				childQueryPlan
		);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		child.resolve(context);
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return child.getResultInfos();
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		child.visit(visitor);
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		final CDateSet restriction = CDateSet.create();
		restriction.maskedAdd(CDateRange.of(dateRange), context.getDateRestriction());

		return getChild().collectRequiredEntities(context.withDateRestriction(restriction));
	}
}
