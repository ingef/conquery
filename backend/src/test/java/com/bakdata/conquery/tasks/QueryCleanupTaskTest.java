package com.bakdata.conquery.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;

@TestInstance(Lifecycle.PER_CLASS)
class QueryCleanupTaskTest {

	private final Duration queryExpiration = Duration.ofDays(30);

	private ManagedQuery createManagedQuery() {
		final CQAnd root = new CQAnd();
		root.setChildren(new ArrayList<>());

		ConceptQuery query = new ConceptQuery(root);

		final ManagedQuery managedQuery = new ManagedQuery(query, null, new DatasetId("test"));

		managedQuery.setCreationTime(LocalDateTime.now().minus(queryExpiration).minusDays(1));

		executions.put(managedQuery.getId(), managedQuery);

		return managedQuery;
	}

	private MetaStorage storageMock;
	private Map<ManagedExecutionId, ? super ManagedExecution<?>> executions;
	private Map<UserId,User> users;

	@BeforeAll
	void setUpAllTests() {
		storageMock = Mockito.mock(MetaStorage.class);

		executions = new HashMap<>();
		users = new HashMap<>();

		// Mock removing execution
		doAnswer(invocation -> {
			final ManagedExecutionId id = invocation.getArgument(0);
			executions.remove(id);
			return null;
		}).when(storageMock).removeExecution(any());
		doAnswer(invocation -> {
			final ManagedExecutionId id = invocation.getArgument(0);
			return executions.get(id);
		}).when(storageMock).getExecution(any());
		doReturn(executions.values()).when(storageMock).getAllExecutions();
		
		// Mock updating user
		doAnswer(invocation -> {
			final User user = invocation.getArgument(0);
			users.put(user.getId(), user);

			return null;
		}).when(storageMock).updateUser(any());

		doReturn(users.values()).when(storageMock).getAllUsers();

	}

	@BeforeEach
	void setUpEachTest() {

		executions.clear();
		users.clear();
	}

	@Test
	void emptyIsEmpty() {
		assertThat(storageMock.getAllExecutions()).isEmpty();
	}

	@Test
	void singleUnnamed() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		createManagedQuery();

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(QueryCleanupTask.EXPIRATION_PARAM, List.of("PT719H")), null);

		assertThat(executions).isEmpty();
	}

	@Test
	void singleNamed() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		managedQuery.setLabel("test");

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

		assertThat(executions.values()).containsExactlyInAnyOrder(managedQuery);
	}

	@Test
	void singleNamedButUUID() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		managedQuery.setLabel(UUID.randomUUID().toString());

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

		assertThat(executions.values()).isEmpty();
	}

	@Test
	void reusedNoNames() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

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

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

		assertThat(executions.values()).containsExactlyInAnyOrder(managedQuery, managedQueryReused);
	}

	@Test
	void reusedNames() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setLabel("test2");

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

		assertThat(executions.values()).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedOtherName() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		managedQuery.setLabel("test2");

		final ManagedQuery managedQueryReused = createManagedQuery();

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

		assertThat(executions.values()).containsExactlyInAnyOrder(managedQueryReused, managedQuery);
	}

	@Test
	void reusedTagged() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setTags(new String[] { "tag" });

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

		assertThat(executions.values()).containsExactlyInAnyOrder(managedQueryReused);
	}

	@Test
	void reusedYoung() throws Exception {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();

		final ManagedQuery managedQueryReused = createManagedQuery();
		managedQueryReused.setCreationTime(LocalDateTime.now());

		managedQuery.setQuery(new ConceptQuery(new CQReusedQuery(managedQueryReused.getId())));

		new QueryCleanupTask(storageMock, queryExpiration).execute( Map.of(), null);

		assertThat(executions.values()).containsExactlyInAnyOrder(managedQueryReused);
	}
	
	@Test
	void doNotDeletePermissionValidReference() {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		// Saving the Execution
		User user = new User("test", "test");
		storageMock.updateUser(user);
		user.addPermission(storageMock, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, managedQuery.getId()));
		
		QueryCleanupTask.deleteQueryPermissionsWithMissingRef(storageMock, storageMock.getAllUsers());
		
		assertThat(user.getPermissions()).containsOnly(QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, managedQuery.getId()));
		
	}
	
	@Test
	void doDeletePermissionInvalidReference() {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery = createManagedQuery();
		// Removing the execution
		storageMock.removeExecution(managedQuery.getId());
		User user = new User("test", "test");
		storageMock.updateUser(user);
		user.addPermission(storageMock, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, managedQuery.getId()));
		
		QueryCleanupTask.deleteQueryPermissionsWithMissingRef(storageMock, storageMock.getAllUsers());
		
		assertThat(user.getPermissions()).isEmpty();
		
	}
	
	@Test
	void doDeletePartialPermissionWithInvalidReference() {
		assertThat(storageMock.getAllExecutions()).isEmpty();

		final ManagedQuery managedQuery1 = createManagedQuery();
		final ManagedQuery managedQuery2 = createManagedQuery();
		// Removing the second execution
		storageMock.removeExecution(managedQuery2.getId());
		User user = new User("test", "test");
		storageMock.updateUser(user);
		user.addPermission(
			storageMock,
			// Build a permission with multiple instances
			new WildcardPermission(List.of(
				Set.of(QueryPermission.DOMAIN),
				Set.of(Ability.READ.toString().toLowerCase()),
				Set.of(managedQuery1.getId().toString(), managedQuery2.getId().toString())), Instant.now()));

		QueryCleanupTask.deleteQueryPermissionsWithMissingRef(storageMock, storageMock.getAllUsers());
		
		assertThat(user.getPermissions()).containsOnly(QueryPermission.onInstance(Ability.READ, managedQuery1.getId()));
		
	}

}