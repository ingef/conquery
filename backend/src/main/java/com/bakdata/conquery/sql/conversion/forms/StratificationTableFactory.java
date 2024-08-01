package com.bakdata.conquery.sql.conversion.forms;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class StratificationTableFactory {

	private final int INDEX_START = 1;
	private final int INDEX_END = 10_000;

	private final QueryStep baseStep;
	private final StratificationFunctions stratificationFunctions;
	private final SqlFunctionProvider functionProvider;

	public StratificationTableFactory(QueryStep baseStep, ConversionContext context) {
		this.baseStep = baseStep;
		this.stratificationFunctions = StratificationFunctions.create(context);
		this.functionProvider = context.getSqlDialect().getFunctionProvider();
	}

	public QueryStep createRelativeStratificationTable(RelativeFormQuery form) {
		RelativeStratification relativeStratification = new RelativeStratification(baseStep, stratificationFunctions, functionProvider);
		return relativeStratification.createRelativeStratificationTable(form);
	}

	public QueryStep createAbsoluteStratificationTable(List<ExportForm.ResolutionAndAlignment> resolutionAndAlignments) {
		AbsoluteStratification absoluteStratification = new AbsoluteStratification(baseStep, stratificationFunctions);
		return absoluteStratification.createStratificationTable(resolutionAndAlignments);
	}

	protected static QueryStep unionResolutionTables(List<QueryStep> unionSteps, List<QueryStep> predecessors) {

		Preconditions.checkArgument(!unionSteps.isEmpty(), "Expecting at least 1 resolution table");

		List<QueryStep> withQualifiedSelects = unionSteps.stream()
														 .map(queryStep -> QueryStep.builder()
																					.selects(queryStep.getQualifiedSelects())
																					.fromTable(QueryStep.toTableLike(queryStep.getCteName()))
																					.build())
														 .toList();

		return QueryStep.createUnionAllStep(
				withQualifiedSelects,
				FormCteStep.FULL_STRATIFICATION.getSuffix(),
				Stream.concat(predecessors.stream(), unionSteps.stream()).toList()
		);
	}

}
