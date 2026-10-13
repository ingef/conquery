package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bakdata.conquery.apiv1.query.CQYes;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import org.junit.jupiter.api.Test;

class SqlConverterTest {

	private final ResolvedQueryAdapter adapter = mock(ResolvedQueryAdapter.class);
	private final ResolvedTableExportAdapter tableExportAdapter = mock(ResolvedTableExportAdapter.class);
	private final ResolvedFormAdapter formAdapter = mock(ResolvedFormAdapter.class);
	private final TranslatorDialect dialect = mock(TranslatorDialect.class);
	private final SqlConverter converter = new SqlConverter(
			adapter, tableExportAdapter, formAdapter, dialect);

	@Test
	void shouldRouteConceptQueriesThroughTheResolvedTranslator() {
		ConceptQuery query = new ConceptQuery(new CQYes());
		query.setResolvedDateAggregationMode(DateAggregationMode.NONE);
		SqlQuery sql = mock(SqlQuery.class);
		when(adapter.translate(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(adapter).translate(same(query), same(dialect));
	}

	@Test
	void shouldRouteSecondaryIdQueriesThroughTheResolvedTranslator() {
		SecondaryIdQuery query = mock(SecondaryIdQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(adapter.translate(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(adapter).translate(same(query), same(dialect));
	}

	@Test
	void shouldRouteTableExportsThroughTheResolvedTranslator() {
		TableExportQuery query = mock(TableExportQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(tableExportAdapter.translate(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(tableExportAdapter).translate(same(query), same(dialect));
	}

	@Test
	void shouldRouteAllFormTypesThroughTheResolvedTranslator() {
		AbsoluteFormQuery absolute = mock(AbsoluteFormQuery.class);
		RelativeFormQuery relative = mock(RelativeFormQuery.class);
		EntityDateQuery entityDate = mock(EntityDateQuery.class);
		SqlQuery absoluteSql = mock(SqlQuery.class);
		SqlQuery relativeSql = mock(SqlQuery.class);
		SqlQuery entityDateSql = mock(SqlQuery.class);
		when(formAdapter.translate(same(absolute), same(dialect))).thenReturn(absoluteSql);
		when(formAdapter.translate(same(relative), same(dialect))).thenReturn(relativeSql);
		when(formAdapter.translate(same(entityDate), same(dialect))).thenReturn(entityDateSql);

		assertSame(absoluteSql, converter.convert(absolute));
		assertSame(relativeSql, converter.convert(relative));
		assertSame(entityDateSql, converter.convert(entityDate));

		verify(formAdapter).translate(same(absolute), same(dialect));
		verify(formAdapter).translate(same(relative), same(dialect));
		verify(formAdapter).translate(same(entityDate), same(dialect));
	}

	@Test
	void shouldRejectUnsupportedQueryDescriptions() {
		QueryDescription query = mock(QueryDescription.class);

		assertThrows(UnsupportedOperationException.class, () -> converter.convert(query));

		verifyNoInteractions(adapter, tableExportAdapter, formAdapter);
	}
}
