package com.bakdata.conquery.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class QueryCleanupTaskTest {

	private final Duration queryExpiration = Duration.ofDays(30);



	private ManagedQuery createManagedQuery() {
		final CQAnd root = new CQAnd();
		root.setChildren(new ArrayList<>());

		ConceptQuery query = new ConceptQuery(root);

		final ManagedQuery managedQuery = new ManagedQuery(query, null, new Dataset("test"));

		managedQuery.setCreationTime(LocalDateTime.now().minus(queryExpiration).minusDays(1));

		storage.addExecution(managedQuery);

		return managedQuery;
	}

	private MetaStorage storage = new MetaStorage(null, new NonPersistentStoreFactory(), null);

	@AfterEach
	public void teardownAfterEach() {
		storage.clear();
	}

	@Test
	void emptyIsEmpty() {
		assertThat(storage.getAllExecutions()).isEmpty();
	}

	@Test
	void singleUnnamed() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		createManagedQuery();

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(QueryCleanupTask.EXPIRATION_PARAM, List.of("PT719H")), null);

		assertThat(storage.getAllExecutions()).isEmpty();
	}

	@Test
	void singleNamed() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		managedQuery.setLabel("test");

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(), null);

		assertThat(storage.getAllExecutions()).containsExactlyInAnyOrder(managedQuery);
	}

	@Test
	void singleNamedButUUID() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		managedQuery.setLabel(UUID.randomUUID().toString());

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(), null);

		assertThat(storage.getAllExecutions()).isEmpty();
	}

	@Test
	void reusedNoNames() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(), null);

		assertThat(storage.getAllExecutions()).isEmpty();
	}

	@Test
	void reusedBothNames() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		managedQuery.setLabel("test1");

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setLabel("test2");

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storage, queryExpiration).execute(Map.of(), null);

		assertThat(storage.getAllExecutions())
				.containsExactlyInAnyOrder(managedQuery, managedQueryReused);
	}

	@Test
	void reusedNames() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setLabel("test2");

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(), null);

		assertThat(storage.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedOtherName() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		managedQuery.setLabel("test2");

		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(), null);

		assertThat(storage.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused, managedQuery);
	}

	@Test
	void reusedTagged() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setTags(new String[] { "tag" });

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(), null);

		assertThat(storage.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedYoung() throws Exception {
		assertThat(storage.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setCreationTime(LocalDateTime.now());

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storage, queryExpiration).execute( Map.of(), null);

		assertThat(storage.getAllExecutions()).containsExactlyInAnyOrder(managedQueryReused);
	}

}