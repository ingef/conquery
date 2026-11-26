package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.google.common.collect.ImmutableBiMap;
import org.junit.jupiter.api.Test;

class SelectFilterTest {

	@Test
	void shouldAllowCustomValuesWhenConfiguredGlobally() throws Exception {
		final MultiSelectFilter filter = new MultiSelectFilter();
		final ConqueryConfig config = new ConqueryConfig();
		final FrontendFilterConfiguration.Top frontend = new FrontendFilterConfiguration.Top();
		config.getFrontend().setAlwaysAllowCreateValue(true);

		filter.configureFrontend(frontend, config);

		assertThat(frontend.getCreatable()).isTrue();
	}

	@Test
	void shouldAllowCustomValuesWithoutEnabledSearches() throws Exception {
		final MultiSelectFilter filter = new MultiSelectFilter();
		final ColumnId columnId = mock(ColumnId.class);
		final Column column = mock(Column.class);
		final FrontendFilterConfiguration.Top frontend = new FrontendFilterConfiguration.Top();
		filter.setColumn(columnId);
		when(columnId.resolve()).thenReturn(column);
		when(column.isSearchDisabled()).thenReturn(true);

		filter.configureFrontend(frontend, new ConqueryConfig());

		assertThat(frontend.getCreatable()).isTrue();
	}

	@Test
	void shouldIgnoreDisabledTemplateAndColumnSearches() throws Exception {
		final MultiSelectFilter filter = new MultiSelectFilter();
		final ColumnId columnId = mock(ColumnId.class);
		final Column column = mock(Column.class);
		final SearchIndexId templateId = mock(SearchIndexId.class);
		final FilterTemplate template = mock(FilterTemplate.class);
		final FrontendFilterConfiguration.Top frontend = new FrontendFilterConfiguration.Top();
		filter.setColumn(columnId);
		filter.setTemplate(templateId);
		when(columnId.resolve()).thenReturn(column);
		when(column.isSearchDisabled()).thenReturn(true);
		when(templateId.resolve()).thenReturn(template);
		when(template.isSearchDisabled()).thenReturn(true);

		filter.configureFrontend(frontend, new ConqueryConfig());

		assertThat(frontend.getCreatable()).isTrue();
		assertThat(filter.getSearchReferences()).isEmpty();
	}

	@Test
	void shouldExposeEnabledTemplateAsSearchSource() throws Exception {
		final MultiSelectFilter filter = new MultiSelectFilter();
		final ColumnId columnId = mock(ColumnId.class);
		final Column column = mock(Column.class);
		final SearchIndexId templateId = mock(SearchIndexId.class);
		final FilterTemplate template = mock(FilterTemplate.class);
		final FrontendFilterConfiguration.Top frontend = new FrontendFilterConfiguration.Top();
		filter.setColumn(columnId);
		filter.setTemplate(templateId);
		when(columnId.resolve()).thenReturn(column);
		when(column.isSearchDisabled()).thenReturn(true);
		when(templateId.resolve()).thenReturn(template);

		filter.configureFrontend(frontend, new ConqueryConfig());

		assertThat(frontend.getTemplate()).isSameAs(template);
		assertThat(frontend.getCreatable()).isFalse();
		assertThat(filter.getSearchReferences()).containsExactly(template);
	}

	@Test
	void shouldRejectLabelsCombinedWithTemplate() {
		final MultiSelectFilter filter = new MultiSelectFilter();
		filter.setLabels(ImmutableBiMap.of("value", "Label"));
		filter.setTemplate(mock(SearchIndexId.class));

		assertThat(filter.isNotUsingTemplateAndLabels()).isFalse();
	}

	@Test
	void shouldAcceptSubstringRangeWithoutMinimum() {
		final MultiSelectFilter filter = new MultiSelectFilter();
		filter.setSubstringRange(new Range.IntegerRange(null, 2));

		assertThat(filter.isMinPositive()).isTrue();
	}

	@Test
	void shouldRejectNegativeSubstringMinimum() {
		final MultiSelectFilter filter = new MultiSelectFilter();
		filter.setSubstringRange(new Range.IntegerRange(-1, 2));

		assertThat(filter.isMinPositive()).isFalse();
	}
}
