package com.bakdata.conquery.sql.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

class SqlExecutionServiceTest {

	@Test
	void shouldAddSortedMetadataComments() {
		final AtomicReference<String> executedSql = new AtomicReference<>();
		final DSLContext dslContext = createDslContext(executedSql, new MockResult(0));
		final SqlExecutionService executionService = new SqlExecutionService(dslContext, mock(ResultSetProcessor.class));

		executionService.withMetadata(Map.of("zeta", "last", "alpha", "first")).execute("select 1");

		assertThat(executedSql.get()).isEqualTo("-- alpha=first\n-- zeta=last\nselect 1");
	}

	@Test
	void shouldEscapeMetadataLineBreaks() {
		final AtomicReference<String> executedSql = new AtomicReference<>();
		final DSLContext dslContext = createDslContext(executedSql, new MockResult(0));
		final SqlExecutionService executionService = new SqlExecutionService(dslContext, mock(ResultSetProcessor.class));

		executionService.withMetadata(Map.of("key\nnext", "value\r\nnext")).execute("select 1");

		assertThat(executedSql.get()).isEqualTo("-- key\\nnext=value\\r\\nnext\nselect 1");
	}

	@Test
	void shouldLeaveSqlUnchangedForEmptyMetadata() {
		final AtomicReference<String> executedSql = new AtomicReference<>();
		final DSLContext dslContext = createDslContext(executedSql, new MockResult(0));
		final SqlExecutionService executionService = new SqlExecutionService(dslContext, mock(ResultSetProcessor.class));

		final DSLContext unmodifiedContext = executionService.withMetadata(Map.of());
		unmodifiedContext.execute("select 1");

		assertThat(unmodifiedContext).isSameAs(dslContext);
		assertThat(executedSql.get()).isEqualTo("select 1");
	}

	@Test
	void shouldProcessResultSetWithMetadata() {
		final AtomicReference<String> executedSql = new AtomicReference<>();
		final DSLContext resultDslContext = DSL.using(SQLDialect.DEFAULT);
		final Result<?> result = resultDslContext.newResult(field(name("id"), String.class));
		final DSLContext dslContext = createDslContext(executedSql, new MockResult(0, result));
		final SqlExecutionService executionService = new SqlExecutionService(dslContext, mock(ResultSetProcessor.class));
		final SqlQuery sqlQuery = mock(SqlQuery.class);
		when(sqlQuery.getSql()).thenReturn("select id");
		when(sqlQuery.getResultInfos()).thenReturn(List.of());

		final SqlExecutionExecutionInfo executionInfo = executionService.execute(sqlQuery, Map.of("queryId", "dataset.execution"));

		assertThat(executedSql.get()).isEqualTo("-- queryId=dataset.execution\nselect id");
		assertThat(executionInfo.getColumnNames()).containsExactly("id");
		assertThat(executionInfo.getTable()).isEmpty();
	}

	private static DSLContext createDslContext(AtomicReference<String> executedSql, MockResult result) {
		return DSL.using(new MockConnection(context -> {
			executedSql.set(context.sql());
			return new MockResult[]{result};
		}), SQLDialect.DEFAULT);
	}
}
