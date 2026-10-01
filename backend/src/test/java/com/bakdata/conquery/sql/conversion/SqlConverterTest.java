package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.CQYes;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import org.junit.jupiter.api.Test;

class SqlConverterTest {

	private final NodeConversions nodeConversions = mock(NodeConversions.class);
	private final ConqueryConfig config = mock(ConqueryConfig.class);
	private final ResolvedQueryMapper mapper = mock(ResolvedQueryMapper.class);
	private final ResolvedQueryAdapter adapter = mock(ResolvedQueryAdapter.class);
	private final LegacyCompilerDialect dialect = mock(LegacyCompilerDialect.class);
	private final SqlConverter converter = new SqlConverter(nodeConversions, config, mapper, adapter, dialect);

	@Test
	void shouldRouteConceptQueriesThroughTheResolvedCompiler() {
		ConceptQuery query = new ConceptQuery(new CQYes());
		query.setResolvedDateAggregationMode(DateAggregationMode.NONE);
		ResolvedQuery resolved = mock(ResolvedQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(mapper.map(same(query), eq(Optional.empty()))).thenReturn(resolved);
		when(adapter.compile(resolved, dialect, List.of())).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(mapper).map(same(query), eq(Optional.empty()));
		verify(adapter).compile(resolved, dialect, List.of());
		verifyNoInteractions(nodeConversions);
	}

	@Test
	void shouldRouteSecondaryIdQueriesThroughTheResolvedCompiler() {
		SecondaryIdQuery query = mock(SecondaryIdQuery.class);
		ConceptQuery nested = mock(ConceptQuery.class);
		SecondaryIdDescriptionId secondaryId = mock(SecondaryIdDescriptionId.class);
		SecondaryIdDescription resolvedSecondaryId = mock(SecondaryIdDescription.class);
		ResolvedQuery resolved = mock(ResolvedQuery.class);
		SqlQuery sql = mock(SqlQuery.class);
		when(query.getQuery()).thenReturn(nested);
		when(query.getSecondaryId()).thenReturn(secondaryId);
		when(secondaryId.resolve()).thenReturn(resolvedSecondaryId);
		when(query.getResultInfos()).thenReturn(List.of());
		when(mapper.map(nested, Optional.of(resolvedSecondaryId), List.of())).thenReturn(resolved);
		when(adapter.compile(resolved, dialect, List.of())).thenReturn(sql);

		SqlQuery converted = converter.convert(query);

		assertSame(sql, converted);
		verify(mapper).map(nested, Optional.of(resolvedSecondaryId), List.of());
		verify(adapter).compile(resolved, dialect, List.of());
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
		verifyNoInteractions(mapper, adapter);
	}
}
