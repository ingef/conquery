package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.ResolvedQuerySource;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import org.junit.jupiter.api.Test;

class ResolvedQuerySourceMapperTest {

	private final ResolvedQueryMapper queryMapper = mock(ResolvedQueryMapper.class);
	private final ResolvedFormMapper formMapper = mock(ResolvedFormMapper.class);
	private final ResolvedTableExportMapper tableExportMapper = mock(ResolvedTableExportMapper.class);
	private final ResolvedQuerySourceMapper mapper = new ResolvedQuerySourceMapper(
			queryMapper, formMapper, tableExportMapper);

	@Test
	void shouldMapStandardAndArrayQuerySources() {
		ConceptQuery concept = mock(ConceptQuery.class);
		SecondaryIdQuery secondary = mock(SecondaryIdQuery.class);
		ResolvedQuery resolvedConcept = mock(ResolvedQuery.class);
		ResolvedQuery resolvedSecondary = mock(ResolvedQuery.class);
		when(queryMapper.map(eq(concept), eq(List.of()))).thenReturn(resolvedConcept);
		when(queryMapper.map(eq(secondary), eq(List.of()))).thenReturn(resolvedSecondary);

		assertSame(resolvedConcept, assertInstanceOf(
				ResolvedQuerySource.Standard.class, mapper.map(concept)).query());
		assertSame(resolvedSecondary, assertInstanceOf(
				ResolvedQuerySource.Standard.class, mapper.map(secondary)).query());

		ArrayConceptQuery array = mock(ArrayConceptQuery.class);
		when(array.getChildQueries()).thenReturn(List.of(concept));
		when(array.getResolvedDateAggregationMode()).thenReturn(DateAggregationMode.MERGE);
		when(resolvedConcept.entitySchema()).thenReturn(mock(com.bakdata.conquery.sql.model.schema.EntitySchema.class));
		ResolvedQuerySource.Union union = assertInstanceOf(ResolvedQuerySource.Union.class, mapper.map(array));
		assertInstanceOf(ResolvedQuerySource.Standard.class, union.children().getFirst());
	}

	@Test
	void shouldRecursivelyMapFormsAndTableExports() {
		ConceptQuery prerequisite = mock(ConceptQuery.class);
		ResolvedQuery resolvedPrerequisite = mock(ResolvedQuery.class);
		when(queryMapper.map(eq(prerequisite), eq(List.of()))).thenReturn(resolvedPrerequisite);

		TableExportQuery export = mock(TableExportQuery.class);
		ResolvedTableExportQuery resolvedExport = mock(ResolvedTableExportQuery.class);
		when(export.getQuery()).thenReturn(prerequisite);
		when(tableExportMapper.map(eq(export), any(ResolvedQuerySource.class))).thenReturn(resolvedExport);
		assertSame(resolvedExport, assertInstanceOf(
				ResolvedQuerySource.TableExport.class, mapper.map(export)).query());

		AbsoluteFormQuery absolute = mock(AbsoluteFormQuery.class);
		RelativeFormQuery relative = mock(RelativeFormQuery.class);
		EntityDateQuery entityDate = mock(EntityDateQuery.class);
		ResolvedFormQuery resolvedForm = mock(ResolvedFormQuery.class);
		when(absolute.getQuery()).thenReturn(export);
		when(relative.getQuery()).thenReturn(prerequisite);
		when(entityDate.getQuery()).thenReturn(prerequisite);
		when(formMapper.map(eq(absolute), any(ResolvedQuerySource.class))).thenReturn(resolvedForm);
		when(formMapper.map(eq(relative), any(ResolvedQuerySource.class))).thenReturn(resolvedForm);
		when(formMapper.map(eq(entityDate), any(ResolvedQuerySource.class))).thenReturn(resolvedForm);

		assertSame(resolvedForm, assertInstanceOf(ResolvedQuerySource.Form.class, mapper.map(absolute)).query());
		assertSame(resolvedForm, assertInstanceOf(ResolvedQuerySource.Form.class, mapper.map(relative)).query());
		assertSame(resolvedForm, assertInstanceOf(ResolvedQuerySource.Form.class, mapper.map(entityDate)).query());
	}
}
