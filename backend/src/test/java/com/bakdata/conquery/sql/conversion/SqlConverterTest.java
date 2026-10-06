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
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import org.junit.jupiter.api.Test;

class SqlConverterTest {

	private final ResolvedQueryAdapter adapter = mock(ResolvedQueryAdapter.class);
	private final ResolvedTableExportAdapter tableExportAdapter = mock(ResolvedTableExportAdapter.class);
	private final ResolvedFormAdapter formAdapter = mock(ResolvedFormAdapter.class);
	private final CompilerDialect dialect = mock(CompilerDialect.class);
	private final SqlConverter converter = new SqlConverter(
			adapter, tableExportAdapter, formAdapter, dialect);

	@Test
	void shouldRouteConceptQueriesThroughTheResolvedCompiler() {
		ConceptQuery query = new ConceptQuery(new CQYes());
		query.setResolvedDateAggregationMode(DateAggregationMode.NONE);
		SqlQuery sql = mock(SqlQuery.class);
		when(adapter.compile(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(adapter).compile(same(query), same(dialect));
	}

	@Test
	void shouldRouteSecondaryIdQueriesThroughTheResolvedCompiler() {
		SecondaryIdQuery query = mock(SecondaryIdQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(adapter.compile(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(adapter).compile(same(query), same(dialect));
	}

	@Test
	void shouldRouteTableExportsThroughTheResolvedCompiler() {
		TableExportQuery query = mock(TableExportQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(tableExportAdapter.compile(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(tableExportAdapter).compile(same(query), same(dialect));
	}

	@Test
	void shouldRouteAllFormTypesThroughTheResolvedCompiler() {
		AbsoluteFormQuery absolute = mock(AbsoluteFormQuery.class);
		RelativeFormQuery relative = mock(RelativeFormQuery.class);
		EntityDateQuery entityDate = mock(EntityDateQuery.class);
		SqlQuery absoluteSql = mock(SqlQuery.class);
		SqlQuery relativeSql = mock(SqlQuery.class);
		SqlQuery entityDateSql = mock(SqlQuery.class);
		when(formAdapter.compile(same(absolute), same(dialect))).thenReturn(absoluteSql);
		when(formAdapter.compile(same(relative), same(dialect))).thenReturn(relativeSql);
		when(formAdapter.compile(same(entityDate), same(dialect))).thenReturn(entityDateSql);

		assertSame(absoluteSql, converter.convert(absolute));
		assertSame(relativeSql, converter.convert(relative));
		assertSame(entityDateSql, converter.convert(entityDate));

		verify(formAdapter).compile(same(absolute), same(dialect));
		verify(formAdapter).compile(same(relative), same(dialect));
		verify(formAdapter).compile(same(entityDate), same(dialect));
	}

	@Test
	void shouldRejectUnsupportedQueryDescriptions() {
		QueryDescription query = mock(QueryDescription.class);

		assertThrows(UnsupportedOperationException.class, () -> converter.convert(query));

		verifyNoInteractions(adapter, tableExportAdapter, formAdapter);
	}
}
