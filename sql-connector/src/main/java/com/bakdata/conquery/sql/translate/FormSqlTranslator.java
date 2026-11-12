package com.bakdata.conquery.sql.translate;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.model.form.ResolvedFormMode;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.form.FormCteStep;
import com.bakdata.conquery.sql.translate.form.StratificationTableFactory;
import com.bakdata.conquery.sql.translate.ir.JoinMode;
import com.bakdata.conquery.sql.translate.ir.ProjectionMode;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SharedAliases;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.translate.query.QueryStepComposer;
import com.bakdata.conquery.sql.translate.query.QueryStepJoiner;
import com.bakdata.conquery.sql.translate.query.ResolvedQueryStepTranslator;
import com.bakdata.conquery.sql.translate.rendering.QueryStepRenderer;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.TableLike;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

/** Compiles fully resolved absolute, relative, and entity-date forms. */
public final class FormSqlTranslator {

	private final QueryStepRenderer renderer;

	public FormSqlTranslator(DSLContext dslContext) {
		this.renderer = new QueryStepRenderer(dslContext);
	}

	public TranslatedQuery translate(ResolvedFormQuery query, TranslatorDialect dialect) {
		SqlNameGenerator names = new SqlNameGenerator(dialect.getNameMaxLength());
		QueryStep finalStep = translateStep(query, dialect, names, new ResolvedQuerySourceStepTranslator(), false);
		Select<Record> rendered = renderer.toSelectQuery(finalStep, dialect);
		return new TranslatedQuery(rendered.getSQL(ParamType.INLINED), TranslatedColumnUtil.create(
				query.prerequisite().entitySchema(), query.resultColumns(), rendered.getSelect()));
	}

	static QueryStep translateStep(
			ResolvedFormQuery query,
			TranslatorDialect dialect,
			SqlNameGenerator names,
			ResolvedQuerySourceStepTranslator sourceTranslator,
			boolean reusable
	) {
		QueryStep prerequisiteRoot = sourceTranslator.translate(query.prerequisite(), dialect, names);
		ResolvedQueryStepTranslator translator = new ResolvedQueryStepTranslator();
		QueryStep formBase = createFormBase(prerequisiteRoot, query.mode(), dialect, names);
		StratificationTableFactory tableFactory = new StratificationTableFactory(formBase, dialect, names);
		QueryStep stratification = switch (query.mode()) {
			case ResolvedFormMode.Absolute absolute -> tableFactory.createAbsoluteStratificationTable(absolute.resolutions(), false);
			case ResolvedFormMode.Relative relative -> tableFactory.createRelativeStratificationTable(relative.settings(), false);
			case ResolvedFormMode.EntityDate entityDate -> tableFactory.createAbsoluteStratificationTable(entityDate.resolutions(), false);
		};

		List<QueryStep> features = query.features().stream()
				.map(feature -> translator.translate(feature, dialect, names, Optional.of(stratification)).step())
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
		return createFinalStep(
				query.mode(), stratification, joinedFeatures, names.reusableStepName("form_result"), reusable);
	}

	private static QueryStep createFormBase(
			QueryStep prerequisite,
			ResolvedFormMode mode,
			TranslatorDialect dialect,
			SqlNameGenerator names
	) {
		return switch (mode) {
			case ResolvedFormMode.Absolute absolute -> createAbsoluteBase(prerequisite, absolute, dialect, names);
			case ResolvedFormMode.Relative ignored -> extractIdsAndValidity(prerequisite, dialect, names);
			case ResolvedFormMode.EntityDate entityDate -> createEntityDateBase(
					extractIdsAndValidity(prerequisite, dialect, names), entityDate, dialect, names);
		};
	}

	private static QueryStep createAbsoluteBase(
			QueryStep prerequisite,
			ResolvedFormMode.Absolute form,
			TranslatorDialect dialect,
			SqlNameGenerator names
	) {
		Selects prerequisiteSelects = prerequisite.getQualifiedSelects();
		ColumnDateRange bounds = dialect.dateRangeLiteral(form.bounds())
				.as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
				.stratificationDate(Optional.of(bounds))
				.build();
		return QueryStep.builder()
				.cteName(names.reusableStepName(FormCteStep.EXTRACT_IDS.getSuffix()))
				.selects(selects)
				.fromTable(QueryStep.toTableLike(prerequisite.getCteName()))
				.groupBy(selects.getIds().toFields())
				.predecessor(prerequisite)
				.build();
	}

	private static QueryStep extractIdsAndValidity(
			QueryStep prerequisite,
			TranslatorDialect dialect,
			SqlNameGenerator names
	) {
		Selects prerequisiteSelects = prerequisite.getQualifiedSelects();
		String cteName = names.reusableStepName(FormCteStep.EXTRACT_IDS.getSuffix());
		Optional<ColumnDateRange> sourceValidity = Optional.of(
				prerequisiteSelects.getValidityDate().orElseGet(dialect::emptyDateRange));
		Selects selects = Selects.builder()
				.ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
				.validityDate(sourceValidity.map(validity -> validity.asValidityDateRange(cteName)))
				.build();
		List<Field<?>> groupBy = Stream.concat(
				Stream.of(prerequisiteSelects.getIds().getPrimaryColumn()),
				sourceValidity.stream().flatMap(range -> range.toFields().stream())
		).toList();
		Condition nonEmpty = sourceValidity.map(dialect::isNotEmptyDateRange).orElseGet(DSL::falseCondition);
		return QueryStep.builder()
				.cteName(cteName)
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
			TranslatorDialect dialect,
			SqlNameGenerator names
	) {
		ColumnDateRange validity = prerequisite.getSelects().getValidityDate()
				.orElseThrow(() -> new IllegalArgumentException("Entity-date forms require a validity date"));
		QueryStep unnested = dialect.supportsSingleColumnRanges()
				? dialect.unnestDateRange(validity, prerequisite,
					names.reusableStepName(FormCteStep.UNNEST_ENTITY_DATE_CTE.getSuffix()))
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
				.cteName(names.reusableStepName(FormCteStep.OVERWRITE_BOUNDS.getSuffix()))
				.selects(selects)
				.fromTable(QueryStep.toTableLike(unnested.getCteName()))
				.predecessors(predecessors)
				.build();
	}

	private static QueryStep createFinalStep(
			ResolvedFormMode mode,
			QueryStep stratification,
			QueryStep joinedFeatures,
			String cteName,
			boolean reusable
	) {
		if (stratification.getSelects().getStratificationDate().isEmpty()
				|| joinedFeatures.getSelects().getStratificationDate().isEmpty()) {
			throw new IllegalArgumentException("Form stratification and features must contain stratification dates");
		}
		Selects featureSelects = joinedFeatures.getQualifiedSelects();
		Selects stratificationSelects = stratification.getQualifiedSelects();
		List<QueryStep> inputs = List.of(stratification, joinedFeatures);
		TableLike<Record> joined = QueryStepJoiner.join(inputs, JoinMode.LEFT);
		if (reusable) {
			Selects selects = Selects.builder()
					.ids(new SqlIdColumns(stratificationSelects.getIds().getPrimaryColumn()))
					.validityDate(featureSelects.getValidityDate()
							.map(validity -> validity.asValidityDateRange(cteName)))
					.build();
			return QueryStep.builder()
					.cteName(cteName)
					.selects(selects)
					.fromTable(joined)
					.selectDistinct(true)
					.predecessors(inputs)
					.build();
		}
		Selects.SelectsBuilder selects = Selects.builder()
				.ids(stratificationSelects.getIds().forFinalSelect(Resolution.COMPLETE.name()))
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
		return QueryStep.builder()
				.cteName(cteName)
				.projectionMode(ProjectionMode.AGGREGATED)
				.selects(selects.build())
				.fromTable(joined)
				.predecessors(inputs)
				.build();
	}
}
