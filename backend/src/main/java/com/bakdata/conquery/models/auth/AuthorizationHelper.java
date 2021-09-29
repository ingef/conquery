package com.bakdata.conquery.models.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.auth.CredentialType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper for easier and cleaner authorization.
 *
 */
@Slf4j
@UtilityClass
public class AuthorizationHelper {

	public static List<Group> getGroupsOf(@NonNull User user, @NonNull MetaStorage storage){

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
	 * the permission of the roles it inherits. The query can be filtered by the Permission domain.
	 * @return Owned and inherited permissions.
	 */
	public static Multimap<String, ConqueryPermission> getEffectiveUserPermissions(User user, List<String> domainSpecifier, MetaStorage storage) {
		Set<ConqueryPermission> permissions = user.getEffectivePermissions();
		Multimap<String, ConqueryPermission> mappedPerms = ArrayListMultimap.create();
		for(ConqueryPermission perm : permissions) {
			Set<String> domains = perm.getDomains();
			if(!Collections.disjoint(domainSpecifier, perm.getDomains())) {
				for(String domain : domains) {
					mappedPerms.put(domain, perm);
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
	public static void authorizeDownloadDatasets(@NonNull User user, @NonNull Visitable visitable) {
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
	public static void authorizeReadDatasets(@NonNull User user, @NonNull Visitable visitable) {
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
	public static Map<DatasetId, Set<Ability>> buildDatasetAbilityMap(User user, DatasetRegistry datasetRegistry) {
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


	public static boolean registerForAuthentication(UserManageable userManager, User user, List<CredentialType> credentials, boolean override) {
		if(override) {
			return userManager.updateUser(user, credentials);
		}
		return userManager.addUser(user, credentials);
	}
}
