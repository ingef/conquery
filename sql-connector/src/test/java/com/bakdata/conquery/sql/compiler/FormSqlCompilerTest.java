package com.bakdata.conquery.sql.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.form.FormAlignment;
import com.bakdata.conquery.sql.model.form.FormCalendarUnit;
import com.bakdata.conquery.sql.model.form.FormIndexPlacement;
import com.bakdata.conquery.sql.model.form.FormIndexSelector;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.RelativeFormSettings;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;
import com.bakdata.conquery.sql.model.form.ResolvedFormMode;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.node.ConceptNode;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.model.result.ResultType;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedConnector;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class FormSqlCompilerTest {

	private static final SqlTable ENTITIES = SqlTable.of("entities", "catalog", "entities");
	private static final ResolvedColumn ENTITY_ID = new ResolvedColumn(
			"entity-id", ENTITIES, "person_id", ColumnType.STRING, false);
	private static final EntitySchema ENTITY_SCHEMA = new EntitySchema(ENTITY_ID);

	@Test
	void shouldCompileAnAbsoluteFormThroughTheConnectorPipeline() {
		ResolvedFormQuery form = new ResolvedFormQuery(
				new ResolvedQuery(ENTITY_SCHEMA, new AllEntitiesNode(), false, List.of()),
				List.of(feature()),
				new ResolvedFormMode.Absolute(
						new DateRange(Optional.of(LocalDate.of(2020, 1, 1)), Optional.of(LocalDate.of(2020, 12, 31))),
						List.of(new ResolutionAndAlignment(FormResolution.COMPLETE, FormAlignment.NO_ALIGN))),
				List.of(
						new ResultColumn("resolution", ResultType.Primitive.STRING),
						new ResultColumn("index", ResultType.Primitive.INTEGER),
						new ResultColumn("range", ResultType.Primitive.DATE_RANGE),
						new ResultColumn("feature", ResultType.Primitive.BOOLEAN))
		);

		CompiledQuery compiled = new FormSqlCompiler(DSL.using(SQLDialect.DEFAULT))
				.compile(form, new HanaCompilerDialect());

		assertTrue(compiled.sql().contains("full_stratification"), compiled.sql());
		assertTrue(compiled.sql().contains("left outer join"), compiled.sql());
		assertEquals(List.of("entity-id", "resolution", "index", "range", "feature"),
				compiled.columns().stream().map(CompiledColumn::outputId).toList());
	}

	@Test
	void shouldCompileRelativeAndEntityDateForms() {
		ResolvedQuery datedPrerequisite = datedQuery("prerequisite", false);
		List<ResolvedQuery> features = List.of(feature("feature"), feature("outcome"));
		FormSqlCompiler compiler = new FormSqlCompiler(DSL.using(SQLDialect.DEFAULT));
		HanaCompilerDialect dialect = new HanaCompilerDialect();
		ResolvedFormQuery relative = new ResolvedFormQuery(
				datedPrerequisite,
				features,
				new ResolvedFormMode.Relative(new RelativeFormSettings(
						FormIndexSelector.EARLIEST,
						FormIndexPlacement.NEUTRAL,
						1,
						1,
						FormCalendarUnit.DAYS,
						List.of(new ResolutionAndAlignment(FormResolution.COMPLETE, FormAlignment.NO_ALIGN)))),
				formResults(true)
		);
		ResolvedFormQuery entityDate = new ResolvedFormQuery(
				datedPrerequisite,
				features,
				new ResolvedFormMode.EntityDate(
						Optional.of(new DateRange(Optional.of(LocalDate.of(2020, 1, 1)), Optional.empty())),
						List.of(new ResolutionAndAlignment(FormResolution.COMPLETE, FormAlignment.NO_ALIGN))),
				formResults(false)
		);

		CompiledQuery relativeCompiled = compiler.compile(relative, dialect);
		CompiledQuery entityDateCompiled = compiler.compile(entityDate, dialect);
		String entityDateSql = entityDateCompiled.sql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

		assertTrue(relativeCompiled.sql().contains("index_selector"), relativeCompiled.sql());
		assertTrue(relativeCompiled.sql().contains("\"scope\""), relativeCompiled.sql());
		assertTrue(entityDateSql.contains(
				"greatest(to_date('2020-01-01', 'yyyy-mm-dd'), \"extract_ids\".\"concept_prerequisite_claims-0_validity_date_start\")"
		), entityDateCompiled.sql());
		assertTrue(entityDateSql.contains(
				"\"overwrite_bounds\".\"stratification_bounds_start\""
		), entityDateCompiled.sql());
		assertEquals(formResults(true).stream().map(ResultColumn::outputId).toList(),
				relativeCompiled.columns().stream().skip(1).map(CompiledColumn::outputId).toList());
		assertEquals(formResults(false).stream().map(ResultColumn::outputId).toList(),
				entityDateCompiled.columns().stream().skip(1).map(CompiledColumn::outputId).toList());
	}

	@Test
	void shouldUseStratificationEndForDateDistanceSelects() {
		ResolvedQuery feature = connectorFeature(
				"age",
				table -> new BuiltInSelects.DateDistance(
						"age",
						new ResolvedColumn("events.birth-date", table, "birth_date", ColumnType.DATE, true),
						ChronoUnit.YEARS,
						LocalDate.of(2023, 3, 28)
				),
				ResultType.Primitive.INTEGER
		);
		String sql = compileAbsoluteForm(feature).sql().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

		assertTrue(sql.contains(
				"years_between(\"events\".\"birth_date\", add_days(\"full_stratification\".\"stratification_bounds_end\", -1))"
		), sql);
	}

	@Test
	void shouldCompileFirstValueSelectWithStratifiedIds() {
		ResolvedQuery feature = connectorFeature(
				"first-value",
				table -> new BuiltInSelects.Values(
						"first-value",
						new ResolvedColumn("events.value", table, "value", ColumnType.STRING, true),
						BuiltInSelects.ValueOperation.FIRST,
						Optional.empty()
				),
				ResultType.Primitive.STRING
		);

		String sql = compileAbsoluteForm(feature).sql().toLowerCase(Locale.ROOT);

		assertTrue(sql.contains("value_select_first_row_step"), sql);
		assertTrue(sql.contains("\"resolution\""), sql);
		assertTrue(sql.contains("\"index\""), sql);
	}

	private static ResolvedQuery feature() {
		return feature("feature");
	}

	private static ResolvedQuery feature(String name) {
		return datedQuery(name, true);
	}

	private static CompiledQuery compileAbsoluteForm(ResolvedQuery feature) {
		ResolvedFormQuery form = new ResolvedFormQuery(
				new ResolvedQuery(ENTITY_SCHEMA, new AllEntitiesNode(), false, List.of()),
				List.of(feature),
				new ResolvedFormMode.Absolute(
						new DateRange(Optional.of(LocalDate.of(2012, 1, 16)), Optional.of(LocalDate.of(2012, 12, 17))),
						List.of(new ResolutionAndAlignment(FormResolution.COMPLETE, FormAlignment.NO_ALIGN))),
				List.of(
						new ResultColumn("resolution", ResultType.Primitive.STRING),
						new ResultColumn("index", ResultType.Primitive.INTEGER),
						new ResultColumn("range", ResultType.Primitive.DATE_RANGE),
						feature.resultColumns().getFirst())
		);
		return new FormSqlCompiler(DSL.using(SQLDialect.DEFAULT)).compile(form, new HanaCompilerDialect());
	}

	private static ResolvedQuery connectorFeature(
			String name,
			Function<SqlTable, ResolvedSelect> selectFactory,
			ResultType.Primitive resultType
	) {
		SqlTable events = SqlTable.of("events", "catalog", "events");
		ResolvedConnector connector = new ResolvedConnector(
				"claims",
				events,
				new ResolvedColumn("events.entity-id", events, "person_id", ColumnType.STRING, false),
				Optional.empty(),
				new ResolvedValidityDate.Point(new ResolvedColumn(
						"events.date", events, "event_date", ColumnType.DATE, true)),
				List.of(),
				List.of(selectFactory.apply(events)),
				List.of()
		);
		return new ResolvedQuery(
				ENTITY_SCHEMA,
				new ConceptNode(name, List.of(connector), List.of(), DateAggregationAction.BLOCK),
				false,
				List.of(new ResultColumn(name, resultType))
		);
	}

	private static ResolvedQuery datedQuery(String name, boolean withSelect) {
		SqlTable events = SqlTable.of("events", "catalog", "events");
		ResolvedConnector connector = new ResolvedConnector(
				"claims",
				events,
				new ResolvedColumn("events.entity-id", events, "person_id", ColumnType.STRING, false),
				Optional.empty(),
				new ResolvedValidityDate.Point(new ResolvedColumn(
						"events.date", events, "event_date", ColumnType.DATE, true)),
				List.of(),
				List.of(),
				List.of()
		);
		return new ResolvedQuery(
				ENTITY_SCHEMA,
				new ConceptNode(name, List.of(connector),
						withSelect ? List.of(new BuiltInSelects.Exists(name)) : List.of(),
						withSelect ? DateAggregationAction.BLOCK : DateAggregationAction.MERGE),
				!withSelect,
				withSelect ? List.of(new ResultColumn(name, ResultType.Primitive.BOOLEAN)) : List.of()
		);
	}

	private static List<ResultColumn> formResults(boolean relative) {
		java.util.ArrayList<ResultColumn> results = new java.util.ArrayList<>(List.of(
				new ResultColumn("resolution", ResultType.Primitive.STRING),
				new ResultColumn("index", ResultType.Primitive.INTEGER)
		));
		if (relative) {
			results.add(new ResultColumn("event-date", ResultType.Primitive.DATE));
		}
		results.add(new ResultColumn("range", ResultType.Primitive.DATE_RANGE));
		if (relative) {
			results.add(new ResultColumn("scope", ResultType.Primitive.STRING));
		}
		results.add(new ResultColumn("feature", ResultType.Primitive.BOOLEAN));
		results.add(new ResultColumn("outcome", ResultType.Primitive.BOOLEAN));
		return results;
	}
}
