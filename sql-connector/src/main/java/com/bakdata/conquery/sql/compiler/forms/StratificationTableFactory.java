package com.bakdata.conquery.sql.compiler.forms;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.model.form.RelativeFormSettings;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;
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
	private final CompilerDialect dialect;

	public StratificationTableFactory(QueryStep baseStep, CompilerDialect dialect) {
		this.baseStep = baseStep;
		this.stratificationFunctions = dialect.stratificationFunctions();
		this.dialect = dialect;
	}

	public QueryStep createRelativeStratificationTable(RelativeFormSettings form, boolean negate) {
		RelativeStratification relativeStratification = new RelativeStratification(baseStep, stratificationFunctions, dialect);
		return relativeStratification.createRelativeStratificationTable(form, negate);
	}

	public QueryStep createAbsoluteStratificationTable(List<ResolutionAndAlignment> resolutionAndAlignments, boolean negate) {
		AbsoluteStratification absoluteStratification = new AbsoluteStratification(baseStep, stratificationFunctions);
		return absoluteStratification.createStratificationTable(resolutionAndAlignments, negate);
	}

	protected static QueryStep unionResolutionTables(List<QueryStep> unionSteps, List<QueryStep> predecessors, boolean negate) {

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
				FormCteStep.FULL_STRATIFICATION.getSuffix(),
				Stream.concat(predecessors.stream(), unionSteps.stream()).toList(),
				negate
		);
	}

}
