package com.bakdata.conquery.sql.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.ConstraintViolationException;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.model.export.ResolvedExportTable;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
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
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.model.result.ResultType;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.sql.validation.ResolvedModelValidation;
import org.junit.jupiter.api.Test;

class ResolvedModelValidationTest {

	private static final SqlTable EVENTS = SqlTable.of("events", "events");
	private static final ResolvedColumn ENTITY_ID = column(EVENTS, "entity-id", "entity_id", ColumnType.STRING);
	private static final ResolvedColumn EVENT_DATE = column(EVENTS, "event-date", "event_date", ColumnType.DATE);
	private static final DateRange BOUNDS = DateRange.closed(
			LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
	private final ResolvedModelValidation validation = new ResolvedModelValidation(ValidationTestSupport.VALIDATOR);

	@Test
	void shouldValidateEveryResolvedModelRoot() {
		assertDoesNotThrow(() -> validation.validate(validQuery()));
		assertDoesNotThrow(() -> validation.validate(validForm()));
		assertDoesNotThrow(() -> validation.validate(validTableExport()));
	}

	@Test
	void shouldRejectInvalidQueriesNestedInFormsAndTableExports() {
		ResolvedQuery invalidQuery = query(column(EVENTS, "numeric-id", "entity_id", ColumnType.INTEGER));
		ResolvedFormQuery form = new ResolvedFormQuery(
				invalidQuery,
				List.of(validQuery()),
				new ResolvedFormMode.Absolute(BOUNDS, List.of(completeResolution())),
				List.of()
		);
		ResolvedTableExportQuery tableExport = new ResolvedTableExportQuery(
				invalidQuery, BOUNDS, List.of(validExportTable()), List.of());

		assertThrows(ConstraintViolationException.class, () -> validation.validate(form));
		assertThrows(ConstraintViolationException.class, () -> validation.validate(tableExport));
	}

	@Test
	void shouldRejectInvalidFormStructureAndResultTypes() {
		ResolvedFormQuery noFeatures = new ResolvedFormQuery(
				validQuery(),
				List.of(),
				new ResolvedFormMode.Absolute(
						BOUNDS,
						List.of(new ResolutionAndAlignment(FormResolution.QUARTERS, FormAlignment.YEAR))
				),
				List.of(new ResultColumn("invalid", new ResultType.ListType(null)))
		);

		ConstraintViolationException exception = assertThrows(
				ConstraintViolationException.class,
				() -> validation.validate(noFeatures)
		);
		Set<String> paths = violationPaths(exception);
		assertTrue(paths.stream().anyMatch(path -> path.equals("features")));
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("compatible")));
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("elementType")));
	}

	@Test
	void shouldRejectNegativeRelativeWindowCounts() {
		ResolvedFormQuery form = new ResolvedFormQuery(
				validQuery(),
				List.of(validQuery()),
				new ResolvedFormMode.Relative(new RelativeFormSettings(
						FormIndexSelector.LATEST,
						FormIndexPlacement.NEUTRAL,
						-1,
						0,
						FormCalendarUnit.DAYS,
						List.of(completeResolution())
				)),
				List.of()
		);

		ConstraintViolationException exception = assertThrows(
				ConstraintViolationException.class,
				() -> validation.validate(form)
		);
		assertTrue(violationPaths(exception).stream().anyMatch(path -> path.endsWith("timeCountBefore")));
	}

	@Test
	void shouldRejectInvalidExportTableColumnsAndDuplicateResultIds() {
		SqlTable other = SqlTable.of("other", "other");
		ResolvedColumn foreignOutput = column(other, "other-value", "value", ColumnType.STRING);
		ResolvedExportTable invalidTable = new ResolvedExportTable(
				"concept",
				"connector",
				EVENTS,
				ENTITY_ID,
				new ResolvedValidityDate.Point(EVENT_DATE),
				"events",
				List.of(Optional.of(foreignOutput)),
				List.of()
		);
		ResolvedTableExportQuery tableExport = new ResolvedTableExportQuery(
				validQuery(),
				BOUNDS,
				List.of(invalidTable),
				List.of(
						new ResultColumn("duplicate", ResultType.Primitive.STRING),
						new ResultColumn("duplicate", ResultType.Primitive.INTEGER)
				)
		);

		ConstraintViolationException exception = assertThrows(
				ConstraintViolationException.class,
				() -> validation.validate(tableExport)
		);
		Set<String> paths = violationPaths(exception);
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("outputColumnsOnExportTable")));
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("resultColumnOutputIdsUnique")));
	}

	@Test
	void shouldReportNullCollectionsAsConstraintViolations() {
		ResolvedFormQuery form = assertDoesNotThrow(() -> new ResolvedFormQuery(
				validQuery(), null,
				new ResolvedFormMode.Absolute(BOUNDS, null),
				null
		));

		ConstraintViolationException exception = assertThrows(
				ConstraintViolationException.class,
				() -> validation.validate(form)
		);
		Set<String> paths = violationPaths(exception);
		assertTrue(paths.contains("features"));
		assertTrue(paths.contains("resultColumns"));
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("resolutions")));
	}

	private static ResolvedFormQuery validForm() {
		return new ResolvedFormQuery(
				validQuery(),
				List.of(validQuery()),
				new ResolvedFormMode.Absolute(BOUNDS, List.of(completeResolution())),
				List.of(new ResultColumn("result", ResultType.Primitive.STRING))
		);
	}

	private static ResolvedTableExportQuery validTableExport() {
		return new ResolvedTableExportQuery(
				validQuery(),
				BOUNDS,
				List.of(validExportTable()),
				List.of(new ResultColumn("result", ResultType.Primitive.STRING))
		);
	}

	private static ResolvedExportTable validExportTable() {
		return new ResolvedExportTable(
				"concept",
				"connector",
				EVENTS,
				ENTITY_ID,
				new ResolvedValidityDate.Point(EVENT_DATE),
				"events",
				List.of(Optional.of(column(EVENTS, "value", "value", ColumnType.STRING))),
				List.of()
		);
	}

	private static ResolutionAndAlignment completeResolution() {
		return new ResolutionAndAlignment(FormResolution.COMPLETE, FormAlignment.NO_ALIGN);
	}

	private static ResolvedQuery validQuery() {
		return query(ENTITY_ID);
	}

	private static ResolvedQuery query(ResolvedColumn entityId) {
		return new ResolvedQuery(new EntitySchema(entityId), new AllEntitiesNode(), false, List.of());
	}

	private static ResolvedColumn column(
			SqlTable table,
			String logicalId,
			String physicalName,
			ColumnType type
	) {
		return new ResolvedColumn(logicalId, table, physicalName, type, false);
	}

	private static Set<String> violationPaths(ConstraintViolationException exception) {
		return exception.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(java.util.stream.Collectors.toSet());
	}
}
