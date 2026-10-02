package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import org.junit.jupiter.api.Test;

class SqlConverterTest {

	private final NodeConversions nodeConversions = mock(NodeConversions.class);
	private final ConqueryConfig config = mock(ConqueryConfig.class);
	private final ResolvedQueryAdapter adapter = mock(ResolvedQueryAdapter.class);
	private final ResolvedTableExportAdapter tableExportAdapter = mock(ResolvedTableExportAdapter.class);
	private final LegacyCompilerDialect dialect = mock(LegacyCompilerDialect.class);
	private final SqlConverter converter = new SqlConverter(nodeConversions, config, adapter, tableExportAdapter, dialect);

	@Test
	void shouldRouteConceptQueriesThroughTheResolvedCompiler() {
		ConceptQuery query = new ConceptQuery(new CQYes());
		query.setResolvedDateAggregationMode(DateAggregationMode.NONE);
		SqlQuery sql = mock(SqlQuery.class);
		when(adapter.compile(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(adapter).compile(same(query), same(dialect));
		verifyNoInteractions(nodeConversions);
	}

	@Test
	void shouldRouteSecondaryIdQueriesThroughTheResolvedCompiler() {
		SecondaryIdQuery query = mock(SecondaryIdQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(adapter.compile(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(adapter).compile(same(query), same(dialect));
		verifyNoInteractions(nodeConversions);
	}

	@Test
	void shouldRouteTableExportsThroughTheResolvedCompiler() {
		TableExportQuery query = mock(TableExportQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(tableExportAdapter.compile(same(query), same(dialect))).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(tableExportAdapter).compile(same(query), same(dialect));
		verifyNoInteractions(nodeConversions);
	}

	@Test
	void shouldKeepOtherQueryDescriptionsOnTheLegacyConversionPath() {
		QueryDescription query = mock(QueryDescription.class);
		ConversionContext context = mock(ConversionContext.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(nodeConversions.convert(query, config)).thenReturn(context);
		when(context.getFinalQuery()).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verifyNoInteractions(adapter, tableExportAdapter);
	}
}
