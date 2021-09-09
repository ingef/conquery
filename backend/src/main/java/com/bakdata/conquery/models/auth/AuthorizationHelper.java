package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.*;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper for easier and cleaner authorization.
 *
 */
@Slf4j
@UtilityClass
public class AuthorizationHelper {

	public static List<Group> getGroupsOf(@NonNull Userish user, @NonNull MetaStorage storage){

		List<Group> userGroups = new ArrayList<>();

		for (Group group : storage.getAllGroups()) {
			if(group.containsMember(user)) {
				userGroups.add(group);
			}
		}
		return userGroups;
	}

	/**
	 * Find the primary group of the user. All users must have a primary group.
	 * @implNote Currently this is the first group of a user and should also be the only group.
	 */
	public static Optional<Group> getPrimaryGroup(@NonNull Userish user, @NonNull MetaStorage storage) {
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
			if (group.containsMember(user)) {
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
	public static void authorizeDownloadDatasets(@NonNull Userish user, @NonNull Visitable visitable) {
		NamespacedIdentifiableCollector collector = new NamespacedIdentifiableCollector();
		visitable.visit(collector);

		Set<Dataset> datasets =
				collector.getIdentifiables()
					.stream()
					.map(NamespacedIdentifiable::getDataset)
					.collect(Collectors.toSet());

		user.authorize(datasets, Ability.DOWNLOAD);
	}

	/**
	 * Checks if a {@link Visitable} has only references to {@link Dataset}s a user is allowed to read.
	 * This checks all used {@link DatasetId}s for the {@link Ability#READ} on the user.
	 */
	public static void authorizeReadDatasets(@NonNull Userish user, @NonNull Visitable visitable) {
		NamespacedIdentifiableCollector collector = new NamespacedIdentifiableCollector();
		visitable.visit(collector);

		Set<Dataset> datasets =
				collector.getIdentifiables()
						 .stream()
						 .map(NamespacedIdentifiable::getDataset)
						 .collect(Collectors.toSet());

		user.authorize(datasets, Ability.READ);
	}


	/**
	 * Calculates the abilities on all datasets a user has based on its permissions.
	 */
	public static Map<DatasetId, Set<Ability>> buildDatasetAbilityMap(Userish user, DatasetRegistry datasetRegistry) {
		HashMap<DatasetId, Set<Ability>> datasetAbilities = new HashMap<>();
		for (Dataset dataset : datasetRegistry.getAllDatasets()) {

			Set<Ability> abilities = datasetAbilities.computeIfAbsent(dataset.getId(), (k) -> new HashSet<>());

			if(user.isPermitted(dataset,Ability.READ)) {
				abilities.add(Ability.READ);
			}

			if (user.isPermitted(dataset,Ability.DOWNLOAD)){
				abilities.add(Ability.DOWNLOAD);
			}

			if (user.isPermitted(dataset,Ability.PRESERVE_ID)) {
				abilities.add(Ability.PRESERVE_ID);
			}
		}
		return datasetAbilities;
	}
}
