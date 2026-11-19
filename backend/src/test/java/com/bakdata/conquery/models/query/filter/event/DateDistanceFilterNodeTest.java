package com.bakdata.conquery.models.query.filter.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import org.junit.jupiter.api.Test;

class DateDistanceFilterNodeTest {

	@Test
	void shouldUseCurrentDateWithoutDateRestriction() {
		final Column column = mock(Column.class);
		final QueryExecutionContext context = mock(QueryExecutionContext.class);
		final Table table = mock(Table.class);
		final Bucket bucket = mock(Bucket.class);
		final LocalDate today = LocalDate.now();
		final DateDistanceFilterNode node = new DateDistanceFilterNode(column, ChronoUnit.YEARS, new Range.LongRange(10L, 10L));

		when(context.getDateRestriction()).thenReturn(CDateSet.createEmpty());
		when(bucket.has(0, column)).thenReturn(true);
		when(bucket.getDate(0, column)).thenReturn(CDate.ofLocalDate(today.minusYears(10)));

		node.nextTable(context, table);

		assertThat(node.checkEvent(bucket, 0)).isTrue();
	}

	@Test
	void shouldUseCurrentDateForOpenEndedDateRestriction() {
		final Column column = mock(Column.class);
		final QueryExecutionContext context = mock(QueryExecutionContext.class);
		final Table table = mock(Table.class);
		final Bucket bucket = mock(Bucket.class);
		final LocalDate today = LocalDate.now();
		final DateDistanceFilterNode node = new DateDistanceFilterNode(column, ChronoUnit.YEARS, new Range.LongRange(10L, 10L));

		when(context.getDateRestriction()).thenReturn(CDateSet.createFull());
		when(bucket.has(0, column)).thenReturn(true);
		when(bucket.getDate(0, column)).thenReturn(CDate.ofLocalDate(today.minusYears(10)));

		node.nextTable(context, table);

		assertThat(node.checkEvent(bucket, 0)).isTrue();
	}
}
