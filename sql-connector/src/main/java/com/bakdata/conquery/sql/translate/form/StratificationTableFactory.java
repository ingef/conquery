package com.bakdata.conquery.sql.translate.form;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.model.form.RelativeFormSettings;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
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
	private final TranslatorDialect dialect;
	private final SqlNameGenerator names;

	public StratificationTableFactory(QueryStep baseStep, TranslatorDialect dialect, SqlNameGenerator names) {
		this.baseStep = baseStep;
		this.stratificationFunctions = dialect.stratificationFunctions();
		this.dialect = dialect;
		this.names = names;
	}

	public QueryStep createRelativeStratificationTable(RelativeFormSettings form, boolean negate) {
		RelativeStratification relativeStratification = new RelativeStratification(
				baseStep, stratificationFunctions, dialect, names);
		return relativeStratification.createRelativeStratificationTable(form, negate);
	}

	public QueryStep createAbsoluteStratificationTable(List<ResolutionAndAlignment> resolutionAndAlignments, boolean negate) {
		AbsoluteStratification absoluteStratification = new AbsoluteStratification(baseStep, stratificationFunctions, names);
		return absoluteStratification.createStratificationTable(resolutionAndAlignments, negate);
	}

	protected static QueryStep unionResolutionTables(
			List<QueryStep> unionSteps,
			List<QueryStep> predecessors,
			boolean negate,
			SqlNameGenerator names
	) {

		if (unionSteps.isEmpty()) {
			throw new IllegalArgumentException("Expecting at least 1 resolution table");
		}

		List<QueryStep> withQualifiedSelects = unionSteps.stream()
														 .map(queryStep -> QueryStep.builder()
																					.selects(queryStep.getQualifiedSelects())
																					.fromTable(QueryStep.toTableLike(queryStep.getCteName()))
																					.build())
														 .toList();

		return QueryStep.createUnionAllStep(
				withQualifiedSelects,
				names.reusableStepName(FormCteStep.FULL_STRATIFICATION.getSuffix()),
				Stream.concat(predecessors.stream(), unionSteps.stream()).toList(),
				negate
		);
	}

}
