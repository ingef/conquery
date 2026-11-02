package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.model.form.FormAlignment;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;
import com.bakdata.conquery.sql.model.form.ResolvedFormMode;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import com.bakdata.conquery.sql.model.node.AllEntitiesNode;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.SqlTable;
import com.bakdata.conquery.sql.translate.FormSqlTranslator;
import com.bakdata.conquery.sql.translate.TableExportSqlTranslator;
import com.bakdata.conquery.sql.translate.dialect.hana.HanaTranslatorDialect;
import org.junit.jupiter.api.Test;

class SqlExtractionValidationTest {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
	private static final HanaTranslatorDialect DIALECT = new HanaTranslatorDialect();

	@Test
	void shouldRejectInvalidResolvedFormBeforeInvokingTranslator() {
		AbsoluteFormQuery form = mock(AbsoluteFormQuery.class);
		ResolvedFormMapper mapper = mock(ResolvedFormMapper.class);
		FormSqlTranslator translator = mock(FormSqlTranslator.class);
		ResolvedFormQuery invalid = new ResolvedFormQuery(
				invalidQuery(),
				List.of(validQuery()),
				new ResolvedFormMode.Absolute(
						DateRange.closed(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
						List.of(new ResolutionAndAlignment(FormResolution.COMPLETE, FormAlignment.NO_ALIGN))
				),
				List.of()
		);
		when(mapper.map(form)).thenReturn(invalid);
		ResolvedFormAdapter adapter = new ResolvedFormAdapter(VALIDATOR, mapper, translator);

		assertThrows(ConstraintViolationException.class, () -> adapter.translate(form, DIALECT));
		verifyNoInteractions(translator);
	}

	@Test
	void shouldRejectInvalidResolvedTableExportBeforeInvokingTranslator() {
		TableExportQuery query = mock(TableExportQuery.class);
		ResolvedTableExportMapper mapper = mock(ResolvedTableExportMapper.class);
		TableExportSqlTranslator translator = mock(TableExportSqlTranslator.class);
		ResolvedTableExportQuery invalid = new ResolvedTableExportQuery(
				invalidQuery(),
				DateRange.closed(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
				List.of(),
				List.of()
		);
		when(mapper.map(query, DIALECT)).thenReturn(invalid);
		ResolvedTableExportAdapter adapter = new ResolvedTableExportAdapter(VALIDATOR, mapper, translator);

		assertThrows(ConstraintViolationException.class, () -> adapter.translate(query, DIALECT));
		verifyNoInteractions(translator);
	}

	private static ResolvedQuery validQuery() {
		return query(ColumnType.STRING);
	}

	private static ResolvedQuery invalidQuery() {
		return query(ColumnType.INTEGER);
	}

	private static ResolvedQuery query(ColumnType idType) {
		SqlTable table = SqlTable.of("entities", "entities");
		ResolvedColumn id = new ResolvedColumn("entity-id", table, "entity_id", idType, false);
		return new ResolvedQuery(new EntitySchema(id), new AllEntitiesNode(), false, List.of());
	}
}
