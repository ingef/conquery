package com.bakdata.conquery.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import io.dropwizard.util.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QueryCleanupTaskTest {

	private final Duration oldQueriesTime = Duration.days(30);

	private ManagedQuery createManagedQuery() {
		final CQAnd root = new CQAnd();
		root.setChildren(new ArrayList<>());

		ConceptQuery query = new ConceptQuery(root);

		final ManagedQuery managedQuery = new ManagedQuery(query, null, new DatasetId("test"));

		managedQuery.setCreationTime(LocalDateTime.now().minusDays(oldQueriesTime.toDays() + 1));

		executions.add(managedQuery);

		return managedQuery;
	}


	private MasterMetaStorage storageMock;
	private List<ManagedExecution> executions;

	@BeforeEach
	void setUp() {
		 storageMock = Mockito.mock(MasterMetaStorage.class);

		 executions = new ArrayList<>();

		doAnswer(invocation -> {
			final ManagedExecutionId id = invocation.getArgument(0);
			executions.removeIf(ex -> ex.getId().equals(id));

			return null;
		}).when(storageMock).removeExecution(any());


		doReturn(executions).when(storageMock).getAllExecutions();
	}

	@Test
	void emptyIsEmpty() {
		assertThat(storageMock.getAllExecutions()).isEmpty();
	}

	@Test
	void singleUnnamed() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		createManagedQuery();

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).isEmpty();
	}

	@Test
	void singleNamed() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		managedQuery.setLabel("test");

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).containsExactlyInAnyOrder(managedQuery);
	}

	@Test
	void reusedNoNames() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).isEmpty();
	}

	@Test
	void reusedBothNames() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		managedQuery.setLabel("test1");

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQuery.setLabel("test2");

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).containsExactlyInAnyOrder(managedQuery, managedQueryReused);
	}

	@Test
	void reusedNames() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setLabel("test2");

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedOtherName() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		managedQuery.setLabel("test2");

		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).containsExactlyInAnyOrder(managedQueryReused, managedQuery);
	}


	@Test
	void reusedTagged() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setTags(new String[]{"tag"});

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedYoung() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setCreationTime(LocalDateTime.now());

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, oldQueriesTime).execute(null, null);

		assertThat(executions).containsExactlyInAnyOrder(managedQueryReused);
	}



}