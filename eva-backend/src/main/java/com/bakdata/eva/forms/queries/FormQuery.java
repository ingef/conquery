package com.bakdata.eva.forms.queries;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.ResultInfo;
import com.bakdata.conquery.models.query.concept.SelectDescriptor;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.eva.EvaConstants;
import com.bakdata.eva.models.forms.DateContext;
import com.fasterxml.jackson.annotation.JsonCreator;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@CPSType(id="FORM_QUERY", base=IQuery.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class FormQuery extends ConceptQuery {

	@Getter @NotNull @NonNull @InternalOnly
	private final Int2ObjectMap<List<DateContext>> includedEntities;
	
	@Override
	public FormQueryPlan createQueryPlan(QueryPlanContext context) {
		context = context.withGenerateSpecialDateUnion(false);
		ConceptQueryPlan subPlan = new ConceptQueryPlan();
		subPlan.setChild(root.createQueryPlan(context, subPlan));

		return new FormQueryPlan(
			subPlan,
			includedEntities,
			new ArrayList<>(subPlan.collectRequiredTables()),
			0
		);
	}
	
	@Override
	public List<ResultInfo> collectResultInfos(PrintSettings config) {
		List<SelectDescriptor> selects = this.collectSelects();
		List<ResultInfo> infos = new ArrayList<>(selects.size() + 2);
		infos.add(EvaConstants.CONTEXT_INDEX_INFO);
		infos.add(EvaConstants.DATE_RANGE_INFO);
		
		return collectResultInfos(this.collectSelects(), infos, config);
	}

	@Override
	public FormQuery resolve(QueryResolveContext context) {
		this.root = root.resolve(context);
		return this;
	}
}
