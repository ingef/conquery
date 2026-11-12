package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;

import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.ResolvedQuerySource;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import org.junit.jupiter.api.Test;

class SqlExtractionTableExportTest {

	@Test
	void shouldMapASecondaryIdQueryAsTableExportPrerequisite() {
		SecondaryIdQuery prerequisite = mock(SecondaryIdQuery.class);
		TableExportQuery query = mock(TableExportQuery.class);
		ResolvedQuery resolvedPrerequisite = mock(ResolvedQuery.class);
		ResolvedQueryMapper queryMapper = mock(ResolvedQueryMapper.class);
		when(query.getQuery()).thenReturn(prerequisite);
		when(query.getDateRange()).thenReturn(Range.all());
		when(query.getConcepts()).thenReturn(List.of());
		when(query.getResultInfos()).thenReturn(List.of());
		when(queryMapper.map(same(prerequisite), eq(List.of()))).thenReturn(resolvedPrerequisite);
		ResolvedTableExportMapper tableExportMapper = new ResolvedTableExportMapper(Clock.systemUTC(), "id");
		ResolvedQuerySourceMapper mapper = new ResolvedQuerySourceMapper(
				queryMapper, mock(ResolvedFormMapper.class), tableExportMapper);

		ResolvedTableExportQuery resolved = mapper.mapTableExport(query);

		assertSame(resolvedPrerequisite, ((ResolvedQuerySource.Standard) resolved.prerequisite()).query());
		verify(queryMapper).map(same(prerequisite), eq(List.of()));
	}
}
