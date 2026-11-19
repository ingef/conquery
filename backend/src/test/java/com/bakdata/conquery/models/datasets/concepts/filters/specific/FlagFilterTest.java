package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.event.FlagColumnsFilterNode;
import org.junit.jupiter.api.Test;

class FlagFilterTest {

	private final FlagFilter filter = new FlagFilter(Map.of("known", mock(ColumnId.class)));

	@Test
	void shouldRejectUnknownFlags() {
		assertThatThrownBy(() -> filter.createFilterNode(Set.of("missing")))
				.isInstanceOf(ConqueryError.ExecutionCreationPlanMissingFlagsError.class);
	}

	@Test
	void shouldRejectEveryEventForEmptySelection() {
		final FlagColumnsFilterNode node = (FlagColumnsFilterNode) filter.createFilterNode(Set.of());
		final Bucket bucket = mock(Bucket.class);

		assertThat(node.checkEvent(bucket, 0)).isFalse();
		verifyNoInteractions(bucket);
	}

	@Test
	void shouldRejectDuplicateFlagColumns() {
		final ColumnId column = mock(ColumnId.class);
		final FlagFilter duplicateColumns = new FlagFilter(Map.of("first", column, "second", column));

		assertThat(duplicateColumns.isAllColumnsOfSameTable()).isFalse();
	}

	@Test
	void shouldAcceptUniqueFlagColumns() {
		final FlagFilter uniqueColumns = new FlagFilter(Map.of("first", mock(ColumnId.class), "second", mock(ColumnId.class)));

		assertThat(uniqueColumns.isAllColumnsOfSameTable()).isTrue();
	}
}
