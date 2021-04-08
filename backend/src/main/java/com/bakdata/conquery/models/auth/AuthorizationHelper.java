package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.RoleOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;

/**
 * Helper for easier and cleaner authorization.
 *
 */
@Slf4j
@UtilityClass
public class AuthorizationHelper {

	public static boolean isPermittedAll(User user, Collection<? extends Authorized> authorized, Ability ability) {
		return authorized.stream()
						 .allMatch(auth -> isPermitted(user, auth, ability));
	}

	public static void authorize(User user, Authorized object, Ability ability) {
		if (isOwnedBy(user, object)) {
			return;
		}

		user.checkPermission(object.createPermission(EnumSet.of(ability)));
	}

	public static boolean isPermitted(User user, Authorized object, Ability ability) {
		if (isOwnedBy(user, object)) {
			return true;
		}

		return user.isPermitted(object.createPermission(EnumSet.of(ability)));
	}

	public static boolean isOwnedBy(User user, Authorized object) {
		return object instanceof Owned && user.isOwner(((Owned) object));
	}

	@Deprecated
	public static boolean isPermitted(User user, ConqueryPermission permission) {
		return user.isPermitted(permission);
	}


	public static boolean[] isPermitted(User user, List<? extends Authorized> authorizeds, Ability ability) {
		return authorizeds.stream()
						  .map(auth -> isPermitted(user, auth, ability))
						  .collect(Collectors.toCollection(BooleanArrayList::new))
						  .toBooleanArray();
	}


	/**
	 * Helper function for authorizing an ability on a query.
	 * @param user The subject that needs authorization.
	 * @param toBeChecked The permission that is checked
	 */
	public static void authorize(@NonNull User user, @NonNull ConqueryPermission toBeChecked) {
		user.checkPermission(toBeChecked);
	}

	/**
	 * Helper function for authorizing an ability on a query.
	 * @param user The subject that needs authorization.
	 * @param toBeChecked The permission that is checked
	 */
	@Deprecated
	public static void authorize(@NonNull User user, @NonNull Set<ConqueryPermission> toBeChecked) {
		user.checkPermissions(Collections.unmodifiableSet(toBeChecked));
	}

	/**
	 * Utility function to add a permission to a subject (e.g {@link User}).
	 * @param owner The subject to own the new permission.
	 * @param permission The permission to add.
	 * @param storage A storage where the permission are added for persistence.
	 */
	public static void addPermission(@NonNull PermissionOwner<?> owner, @NonNull ConqueryPermission permission, @NonNull MetaStorage storage) {
		owner.addPermission(storage, permission);
	}

	/**
	 * Utility function to remove a permission from a subject (e.g {@link User}).
	 * @param owner The subject to own the new permission.
	 * @param permission The permission to remove.
	 * @param storage A storage where the permission is removed from.
	 */
	public static void removePermission(@NonNull PermissionOwner<?> owner, @NonNull Permission permission, @NonNull MetaStorage storage) {
		owner.removePermission(storage, permission);
	}

	public static List<Group> getGroupsOf(@NonNull User user, @NonNull MetaStorage storage){
		final UserId id = user.getId();

		List<Group> userGroups = new ArrayList<>();
		for (Group group : storage.getAllGroups()) {
			if(group.containsMember(id)) {
				userGroups.add(group);
			}
		}
		return userGroups;
	}

	/**
	 * Find the primary group of the user. All users must have a primary group.
	 * @implNote Currently this is the first group of a user and should also be the only group.
	 */
	public static Optional<Group> getPrimaryGroup(@NonNull User user, @NonNull MetaStorage storage) {
		List<Group> groups = getGroupsOf(user, storage);
		if(groups.isEmpty()) {
			return Optional.empty();
		}
		// TODO: 17.02.2020 implement primary flag for user etc.
		return Optional.of(groups.get(0));
	}



	/**
	 * Returns a list of the effective permissions. These are the permissions of the owner and
	 * the permission of the roles it inherits.
	 * @return Owned and inherited permissions.
	 */
	public static Set<ConqueryPermission> getEffectiveUserPermissions(User user, MetaStorage storage) {
		Set<ConqueryPermission> tmpView = collectRolePermissions(storage, user, user.getPermissions());

		for (Group group : storage.getAllGroups()) {
			if (group.containsMember(user.getId())) {
				// Get effective permissions of the group
				tmpView = Sets.union(tmpView, getEffectiveGroupPermissions(group, storage));
			}
		}
		return tmpView;
	}

	/**
	 * Returns a list of the effective permissions. These are the permissions of the owner and
	 * the permission of the roles it inherits.
	 */
	public static Set<ConqueryPermission> getEffectiveGroupPermissions(Group group, MetaStorage storage) {
		// Combine permissions held by the group with those inherited from roles
		return collectRolePermissions(storage, group, group.getPermissions());
	}

	private static Set<ConqueryPermission> collectRolePermissions(MetaStorage storage, RoleOwner roleOwner, Set<ConqueryPermission> tmpView) {
		for (RoleId roleId : roleOwner.getRoles()) {
			Role role = storage.getRole(roleId);
			if (role == null) {
				log.warn("Could not resolve role id [{}]", roleId);
				continue;
			}
			tmpView = Sets.union(tmpView, role.getPermissions());
		}
		return tmpView;
	}


	/**
	 * Returns a list of the effective permissions. These are the permissions of the owner and
	 * the permission of the roles it inherits. The query can be filtered by the Permission domain.
	 * @return Owned and inherited permissions.
	 */
	public static Multimap<String, ConqueryPermission> getEffectiveUserPermissions(User user, List<String> domainSpecifier, MetaStorage storage) {
		Set<ConqueryPermission> permissions = getEffectiveUserPermissions(user, storage);
		Multimap<String, ConqueryPermission> mappedPerms = ArrayListMultimap.create();
		for(Permission perm : permissions) {
			ConqueryPermission cPerm = (ConqueryPermission) perm;
			Set<String> domains = cPerm.getDomains();
			if(!Collections.disjoint(domainSpecifier, cPerm.getDomains())) {
				for(String domain : domains) {
					mappedPerms.put(domain, cPerm);
				}
			}
		}
		return mappedPerms;
	}


	public static void addRoleTo(MetaStorage storage, Role role, RoleOwner owner) {
		owner.addRole(storage, role);
		log.trace("Added role {} to {}", role, owner);
	}

	public static void deleteRoleFrom(MetaStorage storage, RoleOwner owner, Role role) {

		owner.removeRole(storage, role);

		log.trace("Deleted role {} from {}", role, owner);
	}

	public static void deleteRole(MetaStorage storage, Role role) {
		log.info("Deleting {}", role);

		for (User user : storage.getAllUsers()) {
			user.removeRole(storage, role);
		}

		for (Group group : storage.getAllGroups()) {
			group.removeRole(storage, role);
		}

		storage.removeRole(role.getId());
	}



	public static List<User> getUsersByRole(MetaStorage storage, Role role) {
		return storage.getAllUsers().stream().filter(u -> u.getRoles().contains(role.getId())).collect(Collectors.toList());
	}

	public static List<Group> getGroupsByRole(MetaStorage storage, Role role) {
		return storage.getAllGroups().stream().filter(g -> g.getRoles().contains(role.getId())).collect(Collectors.toList());
	}

	/**
	 * Checks if an execution is allowed to be downloaded by a user.
	 * This checks all used {@link DatasetId}s for the {@link Ability#DOWNLOAD} on the user.
	 */
	public static void authorizeDownloadDatasets(@NonNull User user, @NonNull ManagedExecution<?> exec) {
		Set<ConqueryPermission> perms =
				exec.getUsedNamespacedIds()
					.stream()
					.map(NamespacedIdentifiable::getDataset)
					.distinct()
					.map(d -> d.createPermission(Ability.READ.asSet()))
					.collect(Collectors.toSet());

		AuthorizationHelper.authorize(user, perms);
	}

	/**
	 * Checks if a {@link Visitable} has only references to {@link Dataset}s a user is allowed to read.
	 * This checks all used {@link DatasetId}s for the {@link Ability#READ} on the user.
	 */
	public static void authorizeReadDatasets(@NonNull User user, @NonNull Visitable visitable) {
		NamespacedIdentifiableCollector collector = new NamespacedIdentifiableCollector();
		visitable.visit(collector);
		List<Permission> perms = collector.getIdentifiables().stream()
										  .map(NamespacedIdentifiable::getDataset)
										  .distinct()
										  .map(d -> d.createPermission(Ability.READ.asSet()))
										  .map(Permission.class::cast)
										  .collect(Collectors.toList());
		user.checkPermissions(perms);
	}


	/**
	 * Calculates the abilities on all datasets a user has based on its permissions.
	 */
	public static Map<DatasetId, Set<Ability>> buildDatasetAbilityMap(User user, DatasetRegistry datasetRegistry) {
		HashMap<DatasetId, Set<Ability>> datasetAbilities = new HashMap<>();
		for (Dataset dataset : datasetRegistry.getAllDatasets()) {

			Set<Ability> abilities = datasetAbilities.computeIfAbsent(dataset.getId(), (k) -> new HashSet<>());

			if(isPermitted(user,dataset,Ability.READ)) {
				abilities.add(Ability.READ);
			}

			if (isPermitted(user,dataset,Ability.DOWNLOAD)){
				abilities.add(Ability.DOWNLOAD);
			}

			if (isPermitted(user,dataset,Ability.PRESERVE_ID)) {
				abilities.add(Ability.PRESERVE_ID);
			}
		}
		return datasetAbilities;
	}



}
