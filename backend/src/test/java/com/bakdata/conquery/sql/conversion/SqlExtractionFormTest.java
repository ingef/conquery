package com.bakdata.conquery.sql.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TemporalSamplerFactory;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.form.FormAlignment;
import com.bakdata.conquery.sql.model.form.FormCalendarUnit;
import com.bakdata.conquery.sql.model.form.FormIndexPlacement;
import com.bakdata.conquery.sql.model.form.FormIndexSelector;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.ResolvedFormMode;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlExtractionFormTest {

	private final ResolvedQueryMapper queryMapper = mock(ResolvedQueryMapper.class);
	private final Query prerequisite = mock(SecondaryIdQuery.class);
	private final ConceptQuery feature = mock(ConceptQuery.class);
	private final ResolvedQuery resolvedPrerequisite = mock(ResolvedQuery.class);
	private final ResolvedQuery resolvedFeature = mock(ResolvedQuery.class);
	private final ArrayConceptQuery features = mock(ArrayConceptQuery.class);
	private final ResolvedFormMapper mapper = new ResolvedFormMapper(queryMapper);

	@BeforeEach
	void setUp() {
		when(features.getChildQueries()).thenReturn(List.of(feature));
		when(feature.getResultInfos()).thenReturn(List.of());
		when(queryMapper.map(same(prerequisite), eq(List.of()))).thenReturn(resolvedPrerequisite);
		when(queryMapper.map(same(feature), eq(Optional.empty()), anyList())).thenReturn(resolvedFeature);
	}

	@Test
	void shouldMapAbsoluteAndEntityDateBoundsWithoutChangingResolutionSettings() {
		ExportForm.ResolutionAndAlignment resolution = ExportForm.ResolutionAndAlignment.of(
				Resolution.COMPLETE, Alignment.NO_ALIGN);
		AbsoluteFormQuery absolute = mock(AbsoluteFormQuery.class);
		when(absolute.getQuery()).thenReturn(prerequisite);
		when(absolute.getFeatures()).thenReturn(features);
		when(absolute.getDateRange()).thenReturn(Range.of(
				LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)));
		when(absolute.getResolutionsAndAlignmentMap()).thenReturn(List.of(resolution));
		EntityDateQuery entityDate = mock(EntityDateQuery.class);
		when(entityDate.getQuery()).thenReturn(prerequisite);
		when(entityDate.getFeatures()).thenReturn(features);
		when(entityDate.getDateRange()).thenReturn(CDateRange.atLeast(LocalDate.of(2021, 1, 1)));
		when(entityDate.getResolutionsAndAlignments()).thenReturn(List.of(resolution));

		ResolvedFormQuery absoluteResolved = mapper.map(absolute);
		ResolvedFormQuery entityDateResolved = mapper.map(entityDate);

		ResolvedFormMode.Absolute absoluteMode = assertInstanceOf(
				ResolvedFormMode.Absolute.class, absoluteResolved.mode());
		assertEquals(Optional.of(LocalDate.of(2020, 1, 1)), absoluteMode.bounds().startInclusive());
		assertEquals(Optional.of(LocalDate.of(2020, 12, 31)), absoluteMode.bounds().endInclusive());
		assertEquals(FormResolution.COMPLETE, absoluteMode.resolutions().getFirst().resolution());
		assertEquals(FormAlignment.NO_ALIGN, absoluteMode.resolutions().getFirst().alignment());
		ResolvedFormMode.EntityDate entityMode = assertInstanceOf(
				ResolvedFormMode.EntityDate.class, entityDateResolved.mode());
		assertEquals(Optional.of(LocalDate.of(2021, 1, 1)), entityMode.bounds().orElseThrow().startInclusive());
		assertEquals(Optional.empty(), entityMode.bounds().orElseThrow().endInclusive());
		assertSame(resolvedPrerequisite, absoluteResolved.prerequisite());
		assertEquals(List.of(resolvedFeature), absoluteResolved.features());
	}

	@Test
	void shouldMapRelativeFeatureAndOutcomeSettings() {
		RelativeFormQuery relative = mock(RelativeFormQuery.class);
		when(relative.getQuery()).thenReturn(prerequisite);
		when(relative.getFeatures()).thenReturn(features);
		when(relative.getIndexSelector()).thenReturn(TemporalSamplerFactory.LATEST);
		when(relative.getIndexPlacement()).thenReturn(IndexPlacement.NEUTRAL);
		when(relative.getTimeCountBefore()).thenReturn(4);
		when(relative.getTimeCountAfter()).thenReturn(2);
		when(relative.getTimeUnit()).thenReturn(CalendarUnit.QUARTERS);
		when(relative.getResolutionsAndAlignmentMap()).thenReturn(List.of(
				ExportForm.ResolutionAndAlignment.of(Resolution.YEARS, Alignment.QUARTER)));

		ResolvedFormMode.Relative mode = assertInstanceOf(
				ResolvedFormMode.Relative.class, mapper.map(relative).mode());

		assertEquals(FormIndexSelector.LATEST, mode.settings().indexSelector());
		assertEquals(FormIndexPlacement.NEUTRAL, mode.settings().indexPlacement());
		assertEquals(FormCalendarUnit.QUARTERS, mode.settings().timeUnit());
		assertEquals(4, mode.settings().timeCountBefore());
		assertEquals(2, mode.settings().timeCountAfter());
		assertEquals(FormResolution.YEARS, mode.settings().resolutions().getFirst().resolution());
		assertEquals(FormAlignment.QUARTER, mode.settings().resolutions().getFirst().alignment());
		verify(queryMapper).map(same(prerequisite), eq(List.of()));
		verify(queryMapper).map(same(feature), eq(Optional.empty()), eq(List.of()));
	}
}
