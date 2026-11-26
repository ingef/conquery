package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.extensions.MetaStorageExtension;
import io.dropwizard.core.setup.Environment;
import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ExecutionPermissionIndexTest {

	@RegisterExtension
	private static final MetaStorageExtension STORAGE_EXTENSION = new MetaStorageExtension(new Environment("ExecutionPermissionIndexTest").metrics());
	private static final MetaStorage STORAGE = STORAGE_EXTENSION.getMetaStorage();

	private final List<UserId> users = new ArrayList<>();
	private final List<RoleId> roles = new ArrayList<>();
	private final List<GroupId> groups = new ArrayList<>();

	@BeforeAll
	static void setUpAuthorization() {
		new AuthorizationController(STORAGE, new ConqueryConfig(), new Environment("ExecutionPermissionIndexTestAuthorization"), null);
	}

	@AfterEach
	void cleanUpAuthorizationEntities() {
		groups.forEach(STORAGE::removeGroup);
		users.forEach(STORAGE::removeUser);
		roles.forEach(STORAGE::removeRole);
	}

	@Test
	void matchesShiroForOwnedSharedUnsharedAndSystemExecutions() {
		final User user = createUser("basic");
		final UserId otherOwner = new UserId("other");
		final ManagedExecution owned = execution(id("dataset-a"), user.getId(), false);
		final ManagedExecution shared = execution(id("dataset-a"), otherOwner, false);
		final ManagedExecution unshared = execution(id("dataset-a"), otherOwner, false);
		final ManagedExecution system = execution(id("dataset-a"), otherOwner, true);

		user.addPermissions(Set.of(
				ExecutionPermission.onInstance(Ability.READ, shared.getId()),
				ExecutionPermission.onInstance(Ability.READ, system.getId())
		));

		final ExecutionPermissionIndex index = ExecutionPermissionIndex.create(user, Ability.READ);
		assertDecisionMatchesShiro(index, user, owned, true);
		assertDecisionMatchesShiro(index, user, shared, true);
		assertDecisionMatchesShiro(index, user, unshared, false);
		assertDecisionMatchesShiro(index, user, system, true);
		assertThat(system.isSystem()).isTrue();
	}

	@Test
	void matchesShiroWildcardSemantics() {
		final ManagedExecutionId first = id("dataset-a");
		final ManagedExecutionId second = id("dataset-b");
		final ManagedExecutionId other = id("dataset-a");
		final UserId owner = new UserId("other");

		final List<PermissionCase> cases = List.of(
				new PermissionCase("queries:read:" + first, true, false, false),
				new PermissionCase("*:read:" + first, true, false, false),
				new PermissionCase("queries:*:" + first, true, false, false),
				new PermissionCase("queries:read:*", true, true, true),
				new PermissionCase("queries", true, true, true),
				new PermissionCase("queries:read", true, true, true),
				new PermissionCase("datasets,queries:delete,read:" + first + "," + second, true, true, false),
				new PermissionCase("queries:delete:*", false, false, false),
				new PermissionCase("datasets:read:*", false, false, false),
				new PermissionCase("queries:read:" + first + ":*", true, false, false),
				new PermissionCase("queries:read:" + first + ":concrete", false, false, false)
		);

		for (int index = 0; index < cases.size(); index++) {
			final PermissionCase permissionCase = cases.get(index);
			final User user = createUser("wildcard-" + index);
			user.addPermission(new WildcardPermission(permissionCase.permission()));

			final ExecutionPermissionIndex permissionIndex = ExecutionPermissionIndex.create(user, Ability.READ);
			assertDecisionMatchesShiro(permissionIndex, user, execution(first, owner, false), permissionCase.first());
			assertDecisionMatchesShiro(permissionIndex, user, execution(second, owner, false), permissionCase.second());
			assertDecisionMatchesShiro(permissionIndex, user, execution(other, owner, false), permissionCase.other());
		}
	}

	@Test
	void matchesShiroForThreePartPermissionCombinations() {
		final ManagedExecutionId permittedId = id("dataset-a");
		final ManagedExecutionId otherId = id("dataset-b");
		final UserId owner = new UserId("other");
		final List<Set<String>> domains = List.of(
				Set.of(ExecutionPermission.DOMAIN),
				Set.of("*"),
				Set.of("datasets"),
				Set.of(ExecutionPermission.DOMAIN.toUpperCase())
		);
		final List<Set<String>> abilities = List.of(
				Set.of("read"),
				Set.of("*"),
				Set.of("delete"),
				Set.of(Ability.READ.toString().toUpperCase())
		);
		final List<Set<String>> instances = List.of(
				Set.of(permittedId.toString()),
				Set.of(permittedId.toString(), otherId.toString()),
				Set.of("*"),
				Set.of(permittedId.toString().toUpperCase())
		);

		int caseNumber = 0;
		for (final Set<String> domain : domains) {
			for (final Set<String> ability : abilities) {
				for (final Set<String> instance : instances) {
					final User user = createUser("combination-" + caseNumber++);
					user.addPermission(new WildcardPermission(List.of(domain, ability, instance), Instant.EPOCH));

					final ExecutionPermissionIndex index = ExecutionPermissionIndex.create(user, Ability.READ);
					assertDecisionMatchesShiro(index, user, execution(permittedId, owner, false));
					assertDecisionMatchesShiro(index, user, execution(otherId, owner, false));
					assertDecisionMatchesShiro(index, user, execution(id("dataset-a"), owner, false));
				}
			}
		}
	}

	@Test
	void matchesShiroForNonCanonicalPermissionShapes() {
		final ManagedExecutionId permittedId = id("dataset-a");
		final UserId owner = new UserId("other");
		final List<List<Set<String>>> permissionParts = List.of(
				List.of(Set.of(ExecutionPermission.DOMAIN)),
				List.of(Set.of(ExecutionPermission.DOMAIN), Set.of("read")),
				List.of(Set.of(ExecutionPermission.DOMAIN), Set.of("read"), Set.of()),
				List.of(Set.of(ExecutionPermission.DOMAIN), Set.of("read"), Set.of("*")),
				List.of(Set.of(ExecutionPermission.DOMAIN), Set.of("read"), Set.of(permittedId.toString()), Set.of("*")),
				List.of(Set.of(ExecutionPermission.DOMAIN), Set.of("read"), Set.of(permittedId.toString()), Set.of("concrete"))
		);

		for (int caseNumber = 0; caseNumber < permissionParts.size(); caseNumber++) {
			final User user = createUser("non-canonical-" + caseNumber);
			user.addPermission(new WildcardPermission(permissionParts.get(caseNumber), Instant.EPOCH));

			final ExecutionPermissionIndex index = ExecutionPermissionIndex.create(user, Ability.READ);
			assertDecisionMatchesShiro(index, user, execution(permittedId, owner, false));
			assertDecisionMatchesShiro(index, user, execution(id("dataset-a"), owner, false));
		}
	}

	@Test
	void includesPermissionsInheritedFromRolesGroupsAndGroupRoles() {
		final User user = createUser("inherited");
		final Role userRole = createRole("user-role");
		final Role groupRole = createRole("group-role");
		final Group group = createGroup("group");
		final UserId owner = new UserId("other");
		final ManagedExecution fromUserRole = execution(id("dataset-a"), owner, false);
		final ManagedExecution fromGroup = execution(id("dataset-a"), owner, false);
		final ManagedExecution fromGroupRole = execution(id("dataset-b"), owner, false);
		final ManagedExecution unshared = execution(id("dataset-b"), owner, false);

		userRole.addPermission(ExecutionPermission.onInstance(Ability.READ, fromUserRole.getId()));
		group.addPermission(ExecutionPermission.onInstance(Ability.READ, fromGroup.getId()));
		groupRole.addPermission(ExecutionPermission.onInstance(Ability.READ, fromGroupRole.getId()));
		user.addRole(userRole.getId());
		group.addRole(groupRole.getId());
		group.addMember(user.getId());

		final ExecutionPermissionIndex sharedIndex = ExecutionPermissionIndex.create(user, Ability.READ);
		assertDecisionMatchesShiro(sharedIndex, user, fromUserRole, true);
		assertDecisionMatchesShiro(sharedIndex, user, fromGroup, true);
		assertDecisionMatchesShiro(sharedIndex, user, fromGroupRole, true);
		assertDecisionMatchesShiro(sharedIndex, user, unshared, false);

		group.removePermission(ExecutionPermission.onInstance(Ability.READ, fromGroup.getId()));
		final ExecutionPermissionIndex unsharedIndex = ExecutionPermissionIndex.create(user, Ability.READ);
		assertDecisionMatchesShiro(unsharedIndex, user, fromGroup, false);
	}

	@Test
	void matchesShiroWithManySharedExecutionPermissions() {
		final User user = createUser("many");
		final Set<ConqueryPermission> permissions = new HashSet<>();
		final List<ManagedExecutionId> ids = new ArrayList<>();

		for (int index = 0; index < 1_000; index++) {
			final ManagedExecutionId id = id(index % 2 == 0 ? "dataset-a" : "dataset-b");
			ids.add(id);
			permissions.add(ExecutionPermission.onInstance(Set.of(Ability.READ, Ability.SHARE), id));
		}
		user.addPermissions(permissions);

		final UserId owner = new UserId("other");
		final ExecutionPermissionIndex index = ExecutionPermissionIndex.create(user, Ability.READ);
		assertDecisionMatchesShiro(index, user, execution(ids.getFirst(), owner, false), true);
		assertDecisionMatchesShiro(index, user, execution(ids.getLast(), owner, false), true);
		assertDecisionMatchesShiro(index, user, execution(id("dataset-a"), owner, false), false);
	}

	@Test
	void fallsBackToPermissionImplicationForOtherPermissionImplementations() {
		final User user = createUser("fallback");
		final ManagedExecutionId permittedId = id("dataset-a");
		final ManagedExecution permitted = execution(permittedId, new UserId("other"), false);
		final ConqueryPermission delegate = ExecutionPermission.onInstance(Ability.READ, permittedId);
		user.addPermission(new DelegatingPermission(delegate));

		final ExecutionPermissionIndex index = ExecutionPermissionIndex.create(user, Ability.READ);
		assertDecisionMatchesShiro(index, user, permitted, true);
		assertDecisionMatchesShiro(index, user, execution(id("dataset-a"), new UserId("other"), false), false);
	}

	@Test
	void delegatesToNonUserSubject() {
		final Subject subject = mock(Subject.class);
		final ManagedExecution execution = execution(id("dataset-a"), new UserId("other"), false);
		when(subject.isPermitted(execution, Ability.READ)).thenReturn(true);

		final ExecutionPermissionIndex index = ExecutionPermissionIndex.create(subject, Ability.READ);

		assertThat(index.isPermitted(execution)).isTrue();
		verify(subject).isPermitted(execution, Ability.READ);
		verify(subject, never()).getUser();
	}

	private User createUser(String name) {
		final User user = new User(name, name, STORAGE);
		STORAGE.addUser(user);
		users.add(user.getId());
		return user;
	}

	private Role createRole(String name) {
		final Role role = new Role(name, name, STORAGE);
		STORAGE.addRole(role);
		roles.add(role.getId());
		return role;
	}

	private Group createGroup(String name) {
		final Group group = new Group(name, name, STORAGE);
		STORAGE.addGroup(group);
		groups.add(group.getId());
		return group;
	}

	private static ManagedExecution execution(ManagedExecutionId id, UserId owner, boolean system) {
		final ManagedExecution execution = mock(ManagedExecution.class);
		when(execution.getId()).thenReturn(id);
		when(execution.getOwner()).thenReturn(owner);
		when(execution.isSystem()).thenReturn(system);
		when(execution.createPermission(anySet())).thenAnswer(invocation -> {
			final Set<Ability> abilities = invocation.getArgument(0);
			return ExecutionPermission.onInstance(abilities, id);
		});
		return execution;
	}

	private static ManagedExecutionId id(String dataset) {
		return new ManagedExecutionId(new DatasetId(dataset), UUID.randomUUID());
	}

	private static void assertDecisionMatchesShiro(
			ExecutionPermissionIndex index,
			User user,
			ManagedExecution execution,
			boolean expected) {
		final boolean shiroDecision = user.isPermitted(execution, Ability.READ);
		assertThat(index.isPermitted(execution)).isEqualTo(shiroDecision).isEqualTo(expected);
	}

	private static void assertDecisionMatchesShiro(
			ExecutionPermissionIndex index,
			User user,
			ManagedExecution execution) {
		final boolean shiroDecision = user.isPermitted(execution, Ability.READ);
		assertThat(index.isPermitted(execution)).isEqualTo(shiroDecision);
	}

	private record PermissionCase(String permission, boolean first, boolean second, boolean other) {
	}

	private record DelegatingPermission(ConqueryPermission delegate) implements ConqueryPermission {
		@Override
		public Set<String> getDomains() {
			return delegate.getDomains();
		}

		@Override
		public Set<String> getAbilities() {
			return delegate.getAbilities();
		}

		@Override
		public Set<String> getInstances() {
			return delegate.getInstances();
		}

		@Override
		public Instant getCreationTime() {
			return delegate.getCreationTime();
		}

		@Override
		public boolean implies(Permission permission) {
			return delegate.implies(permission);
		}
	}
}
