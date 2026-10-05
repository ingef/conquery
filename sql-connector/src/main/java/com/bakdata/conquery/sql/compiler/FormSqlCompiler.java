package com.bakdata.conquery.sql.compiler;

import static org.jooq.impl.DSL.falseCondition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.forms.FormCteStep;
import com.bakdata.conquery.sql.compiler.forms.StratificationTableFactory;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.compiler.ir.ProjectionMode;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStepComposer;
import com.bakdata.conquery.sql.compiler.ir.QueryStepJoiner;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SharedAliases;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.compiler.rendering.QueryStepRenderer;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.ResolvedFormMode;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.TableLike;
import org.jooq.conf.ParamType;

/** Compiles fully resolved absolute, relative, and entity-date forms. */
public final class FormSqlCompiler {

	private final QueryStepRenderer renderer;
	private final ResolvedQueryStepCompiler stepCompiler = new ResolvedQueryStepCompiler();

	public FormSqlCompiler(DSLContext dslContext) {
		this.renderer = new QueryStepRenderer(dslContext);
	}

	public CompiledQuery compile(ResolvedFormQuery query, CompilerDialect dialect) {
		SqlNameGenerator names = new SqlNameGenerator(dialect.getNameMaxLength());
		QueryStep prerequisiteRoot = stepCompiler.compile(
				query.prerequisite(), dialect, names, Optional.empty()).step();
		QueryStep formBase = createFormBase(prerequisiteRoot, query.mode(), dialect);
		StratificationTableFactory tableFactory = new StratificationTableFactory(formBase, dialect);
		QueryStep stratification = switch (query.mode()) {
			case ResolvedFormMode.Absolute absolute -> tableFactory.createAbsoluteStratificationTable(absolute.resolutions(), false);
			case ResolvedFormMode.Relative relative -> tableFactory.createRelativeStratificationTable(relative.settings(), false);
			case ResolvedFormMode.EntityDate entityDate -> tableFactory.createAbsoluteStratificationTable(entityDate.resolutions(), false);
		};

		List<QueryStep> features = query.features().stream()
				.map(feature -> stepCompiler.compile(feature, dialect, names, Optional.of(stratification)).step())
				.toList();
		if (features.isEmpty()) {
			throw new IllegalArgumentException("A form must contain at least one feature query");
		}
		QueryStep joinedFeatures = QueryStepComposer.joinSteps(
				features,
				JoinMode.FULL_OUTER,
				DateAggregationAction.BLOCK,
				query.prerequisite().entitySchema(),
				dialect,
				names
		);
		QueryStep finalStep = createFinalStep(query.mode(), stratification, joinedFeatures);
		Select<Record> rendered = renderer.toSelectQuery(finalStep, dialect);
		return new CompiledQuery(rendered.getSQL(ParamType.INLINED), CompiledQueryColumns.create(
				query.prerequisite().entitySchema(), query.resultColumns(), rendered.getSelect()));
	}

	private static QueryStep createFormBase(QueryStep prerequisite, ResolvedFormMode mode, CompilerDialect dialect) {
		return switch (mode) {
			case ResolvedFormMode.Absolute absolute -> createAbsoluteBase(prerequisite, absolute, dialect);
			case ResolvedFormMode.Relative ignored -> extractIdsAndValidity(prerequisite, dialect);
			case ResolvedFormMode.EntityDate entityDate -> createEntityDateBase(
					extractIdsAndValidity(prerequisite, dialect), entityDate, dialect);
		};
	}

	private static QueryStep createAbsoluteBase(
			QueryStep prerequisite,
			ResolvedFormMode.Absolute form,
			CompilerDialect dialect
	) {
		Selects prerequisiteSelects = prerequisite.getQualifiedSelects();
		ColumnDateRange bounds = dialect.dateRangeLiteral(form.bounds())
				.as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
				.stratificationDate(Optional.of(bounds))
				.build();
		return QueryStep.builder()
				.cteName(FormCteStep.EXTRACT_IDS.getSuffix())
				.selects(selects)
				.fromTable(QueryStep.toTableLike(prerequisite.getCteName()))
				.groupBy(selects.getIds().toFields())
				.predecessor(prerequisite)
				.build();
	}

	private static QueryStep extractIdsAndValidity(QueryStep prerequisite, CompilerDialect dialect) {
		Selects prerequisiteSelects = prerequisite.getQualifiedSelects();
		Optional<ColumnDateRange> validity = prerequisiteSelects.getValidityDate();
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
				.validityDate(validity)
				.build();
		List<Field<?>> groupBy = Stream.concat(
				Stream.of(prerequisiteSelects.getIds().getPrimaryColumn()),
				validity.stream().flatMap(range -> range.toFields().stream())
		).toList();
		Condition nonEmpty = validity.map(dialect::isNotEmptyDateRange).orElseGet(() -> falseCondition());
		return QueryStep.builder()
				.cteName(FormCteStep.EXTRACT_IDS.getSuffix())
				.selects(selects)
				.fromTable(QueryStep.toTableLike(prerequisite.getCteName()))
				.conditions(List.of(nonEmpty))
				.groupBy(groupBy)
				.predecessor(prerequisite)
				.build();
	}

	private static QueryStep createEntityDateBase(
			QueryStep prerequisite,
			ResolvedFormMode.EntityDate form,
			CompilerDialect dialect
	) {
		ColumnDateRange validity = prerequisite.getSelects().getValidityDate()
				.orElseThrow(() -> new IllegalArgumentException("Entity-date forms require a validity date"));
		QueryStep unnested = dialect.supportsSingleColumnRanges()
				? dialect.unnestDateRange(validity, prerequisite, FormCteStep.UNNEST_ENTITY_DATE_CTE.getSuffix())
				: prerequisite;
		Selects unnestedSelects = unnested.getQualifiedSelects();
		ColumnDateRange entityDate = unnestedSelects.getValidityDate().orElseThrow();
		ColumnDateRange bounds = form.bounds()
				.map(dialect::dateRangeLiteral)
				.map(formRange -> dialect.intersection(formRange, entityDate))
				.orElse(entityDate)
				.as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());
		Selects selects = Selects.builder()
				.ids(unnestedSelects.getIds())
				.stratificationDate(Optional.of(bounds))
				.build();
		List<QueryStep> predecessors = unnested == prerequisite
				? List.of(prerequisite)
				: List.of(prerequisite, unnested);
		return QueryStep.builder()
				.cteName(FormCteStep.OVERWRITE_BOUNDS.getSuffix())
				.selects(selects)
				.fromTable(QueryStep.toTableLike(unnested.getCteName()))
				.predecessors(predecessors)
				.build();
	}

	private static QueryStep createFinalStep(
			ResolvedFormMode mode,
			QueryStep stratification,
			QueryStep joinedFeatures
	) {
		if (stratification.getSelects().getStratificationDate().isEmpty()
				|| joinedFeatures.getSelects().getStratificationDate().isEmpty()) {
			throw new IllegalArgumentException("Form stratification and features must contain stratification dates");
		}
		Selects featureSelects = joinedFeatures.getQualifiedSelects();
		Selects stratificationSelects = stratification.getQualifiedSelects();
		Selects.SelectsBuilder selects = Selects.builder()
				.ids(stratificationSelects.getIds().forFinalSelect(FormResolution.COMPLETE.name()))
				.validityDate(featureSelects.getValidityDate())
				.stratificationDate(stratificationSelects.getStratificationDate());
		if (mode instanceof ResolvedFormMode.Relative) {
			Field<Integer> index = field(name(stratification.getCteName(), SharedAliases.INDEX.getAlias()), Integer.class);
			Field<String> scope = when(index.isNull().or(index.lessThan(0)), inline("FEATURE"))
					.otherwise(inline("OUTCOME"))
					.as(SharedAliases.OBSERVATION_SCOPE.getAlias());
			selects.sqlSelect(new FieldWrapper<>(scope));
		}
		selects.sqlSelects(featureSelects.getSqlSelects());
		List<QueryStep> inputs = List.of(stratification, joinedFeatures);
		TableLike<Record> joined = QueryStepJoiner.join(inputs, JoinMode.LEFT);
		return QueryStep.builder()
				.cteName(null)
				.projectionMode(ProjectionMode.AGGREGATED)
				.selects(selects.build())
				.fromTable(joined)
				.predecessors(inputs)
				.build();
	}
}
