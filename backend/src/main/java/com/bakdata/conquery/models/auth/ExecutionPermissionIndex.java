package com.bakdata.conquery.models.auth;

import java.util.*;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.execution.ManagedExecution;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Request-scoped index for permissions on executions.
 *
 * <p>The index narrows the permissions that can apply to an execution while leaving the actual implication decision
 * to the permission implementation. It inverts the permission-to-instance relationship: instead of scanning every
 * effective permission for each execution, it uses a normalized execution ID to look up the permissions that name
 * that instance. Permissions that cannot be represented by this mapping are checked separately as fallbacks.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionPermissionIndex {

	private static final String WILDCARD_TOKEN = "*";

	private final Subject subject;
	private final Ability ability;
	/**
	 * Short-circuit for non-User subjects, because they cannot be indexed the same way.
	 */
	private final boolean indexed;
	/**
	 * Maps a normalized instance from the third part of a wildcard permission to all permissions containing that
	 * instance. A permission can name multiple instances, so the same permission can be stored under multiple keys.
	 * For example, a permission ending in {@code instance-a,instance-b} is stored in both the {@code instance-a} and
	 * {@code instance-b} buckets.
	 */
	private final Map<String, List<ConqueryPermission>> permissionsByInstance;
	/**
	 * Permissions whose shape or semantics require checking them for every execution.
	 */
	private final List<ConqueryPermission> fallbackPermissions;

	public static ExecutionPermissionIndex create(@NonNull Subject subject, @NonNull Ability ability) {
		if (!(subject instanceof User user)) {
			return new ExecutionPermissionIndex(subject, ability, false, Map.of(), List.of());
		}

		final Map<String, List<ConqueryPermission>> permissionsByInstance = new HashMap<>();
		final List<ConqueryPermission> fallbackPermissions = new ArrayList<>();

		for (final ConqueryPermission permission : user.getEffectivePermissions()) {
			if (permission instanceof WildcardPermission wildcardPermission && isIndexable(wildcardPermission)) {
				for (final String instance : wildcardPermission.getParts().get(2)) {
					permissionsByInstance.computeIfAbsent(normalize(instance), ignored -> new ArrayList<>()).add(permission);
				}
			} else {
				fallbackPermissions.add(permission);
			}

		}

		final Map<String, List<ConqueryPermission>> immutablePermissionsByInstance = new HashMap<>();

		for (Map.Entry<String, List<ConqueryPermission>> entry : permissionsByInstance.entrySet()) {
			String instance = entry.getKey();
			List<ConqueryPermission> permissions = entry.getValue();

			immutablePermissionsByInstance.put(instance, List.copyOf(permissions));
		}

		return new ExecutionPermissionIndex(
				subject,
				ability,
				true,
				Map.copyOf(immutablePermissionsByInstance),
				List.copyOf(fallbackPermissions)
		);
	}

	private static boolean isIndexable(WildcardPermission permission) {
		final List<Set<String>> parts = permission.getParts();

		if (parts.size() != 3) {
			return false;
		}

		if (parts.get(2).isEmpty()) {
			return false;
		}

		if (parts.get(2).contains(WILDCARD_TOKEN)) {
			return false;
		}

		return true;
	}

	/**
	 * Shiro's default wildcard permission is case-insensitive and uses {@link String#toLowerCase()}.
	 */
	private static String normalize(String value) {
		return value.toLowerCase();
	}

	public boolean isPermitted(@NonNull ManagedExecution execution) {
		if (!indexed) {
			return subject.isPermitted(execution, ability);
		}

		if (subject.isOwner(execution)) {
			return true;
		}

		final ConqueryPermission requestedPermission = execution.createPermission(ability.asSet());
		final List<ConqueryPermission> candidatePermissions = permissionsByInstance.getOrDefault(
				normalize(execution.getId().toString()),
				List.of()
		);

		for (ConqueryPermission candidatePermission : candidatePermissions) {
			if (candidatePermission.implies(requestedPermission)) {
				return true;
			}
		}

		for (ConqueryPermission permission : fallbackPermissions) {
			if (permission.implies(requestedPermission)) {
				return true;
			}
		}

		return false;
	}
}
