package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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

	@Test
	void shouldPreserveExportLayoutAndUseRequestLocaleForConceptName() {
		final ColumnId columnId = mock(ColumnId.class);
		final TableId tableId = mock(TableId.class);
		final Table physicalTable = mock(Table.class);
		final Column column = mock(Column.class);
		when(tableId.toString()).thenReturn("dataset.events");
		when(physicalTable.getId()).thenReturn(tableId);
		when(physicalTable.getName()).thenReturn("events");
		when(physicalTable.getColumns()).thenReturn(new Column[]{column});
		when(column.getId()).thenReturn(columnId);
		when(column.getTable()).thenReturn(physicalTable);
		when(column.getName()).thenReturn("diagnosis");
		when(column.getType()).thenReturn(MajorTypeId.STRING);

		final Connector connector = mock(Connector.class);
		when(connector.getResolvedTable()).thenReturn(physicalTable);
		when(connector.getName()).thenReturn("claims");
		final ConnectorId connectorId = mock(ConnectorId.class);
		when(connectorId.resolve()).thenReturn(connector);
		final CQTable table = mock(CQTable.class);
		when(table.getConnector()).thenReturn(connectorId);
		when(table.getFilters()).thenReturn(List.of());
		final CQConcept concept = mock(CQConcept.class);
		when(concept.getTables()).thenReturn(List.of(table));
		when(concept.userLabel(Locale.GERMAN)).thenReturn("Diagnosen");

		final TableExportQuery query = mock(TableExportQuery.class);
		when(query.getDateRange()).thenReturn(Range.all());
		when(query.getConcepts()).thenReturn(List.of(concept));
		when(query.getPositions()).thenReturn(Map.of(columnId, 2));
		when(query.getResultInfos()).thenReturn(List.of());
		final ResolvedTableExportMapper mapper = new ResolvedTableExportMapper(Clock.systemUTC(), "person_id");
		final Locale previous = I18n.LOCALE.get();
		try {
			I18n.LOCALE.set(Locale.GERMAN);
			final ResolvedTableExportQuery resolved = mapper.map(
					query, new ResolvedQuerySource.Standard(mock(ResolvedQuery.class)));

			assertEquals("Diagnosen", resolved.tables().getFirst().conceptName());
			assertEquals(2, resolved.tables().getFirst().outputColumns().size());
			assertTrue(resolved.tables().getFirst().outputColumns().getFirst().isEmpty());
			assertEquals("diagnosis", resolved.tables().getFirst().outputColumns().get(1).orElseThrow().physicalName());
		}
		finally {
			I18n.LOCALE.set(previous);
		}
	}
}
