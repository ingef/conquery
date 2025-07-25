package com.bakdata.conquery.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class QueryCleanupTaskTest {
	private static final MetaStorage STORAGE = new NonPersistentStoreFactory().createMetaStorage();

	private final Duration queryExpiration = Duration.ofDays(30);

	@AfterEach
	public void teardownAfterEach() {
		STORAGE.clear();
	}

	@Test
	void emptyIsEmpty() {
		assertThat(STORAGE.getAllExecutions()).isEmpty();
	}

	@Test
	void singleUnnamed() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		createManagedQuery();

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(QueryCleanupTask.EXPIRATION_PARAM, List.of("PT719H")), null);

		assertThat(STORAGE.getAllExecutions()).isEmpty();
	}

	private ManagedQuery createManagedQuery() {
		final CQAnd root = new CQAnd();
		root.setChildren(new ArrayList<>());

		ConceptQuery query = new ConceptQuery(root);

		final ManagedQuery managedQuery = new ManagedQuery(query, new UserId("test"), new DatasetId("test"), STORAGE, null, null);

		managedQuery.setCreationTime(LocalDateTime.now().minus(queryExpiration).minusDays(1));

		STORAGE.addExecution(managedQuery);
		managedQuery.setMetaStorage(STORAGE);

		return managedQuery;
	}

	@Test
	void singleNamed() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		managedQuery.setLabel("test");

		QueryCleanupTask queryCleanupTask = new QueryCleanupTask(STORAGE, queryExpiration);
		queryCleanupTask.execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions()).containsExactlyInAnyOrder(managedQuery);
	}

	@Test
	void singleNamedButAutoLabeled() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		managedQuery.setLabel("test" + ManagedExecution.AUTO_LABEL_SUFFIX);

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions()).isEmpty();
	}

	@Test
	void reusedNoNames() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions()).isEmpty();
	}

	@Test
	void reusedBothNames() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		managedQuery.setLabel("test1");

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setLabel("test2");

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions())
				.containsExactlyInAnyOrder(managedQuery, managedQueryReused);
	}

	@Test
	void reusedNames() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setLabel("test2");

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedOtherName() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		managedQuery.setLabel("test2");

		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused, managedQuery);
	}

	@Test
	void reusedTagged() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setTags(new String[] { "tag" });

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedYoung() throws Exception {
		assertThat(STORAGE.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setCreationTime(LocalDateTime.now());

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(STORAGE, queryExpiration).execute(Map.of(), null);

		assertThat(STORAGE.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused);
	}

}