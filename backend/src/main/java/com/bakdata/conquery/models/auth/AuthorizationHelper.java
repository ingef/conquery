package com.bakdata.conquery.models.auth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.auth.CredentialType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.Visitable;
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

	public static List<Group> getGroupsOf(@NonNull Subject subject, @NonNull MetaStorage storage){
		try(Stream<Group> allGroups = storage.getAllGroups()) {
			return allGroups
					.filter(g -> g.getMembers().contains(subject.getId()))
					.sorted()
					.collect(Collectors.toList());
		}
	}

	/**
	 * Find the primary group of the subject. All users must have a primary group.
	 * @implNote Currently this is the first group of a subject and should also be the only group.
	 */
	public static Optional<Group> getPrimaryGroup(@NonNull Subject subject, @NonNull MetaStorage storage) {
		List<Group> groups = getGroupsOf(subject, storage);
		if(groups.isEmpty()) {
			return Optional.empty();
		}
		// TODO: 17.02.2020 implement primary flag for group
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
		return storage.getAllUsers().filter(u -> u.getRoles().contains(role.getId())).collect(Collectors.toList());
	}

	public static List<Group> getGroupsByRole(MetaStorage storage, Role role) {
		return storage.getAllGroups().filter(g -> g.getRoles().contains(role.getId())).collect(Collectors.toList());
	}

	/**
	 * Checks if an execution is allowed to be downloaded by a user.
	 * This checks all used {@link DatasetId}s for the {@link Ability#DOWNLOAD} on the user.
	 */
	public static void authorizeDownloadDatasets(@NonNull Subject subject, @NonNull Visitable visitable) {
		NamespacedIdentifiableCollector collector = new NamespacedIdentifiableCollector();
		visitable.visit(collector);

		Set<DatasetId> datasets =
				collector.getIdentifiables()
						 .stream()
						 .map(NamespacedIdentifiable::getDataset)
						 .collect(Collectors.toSet());

		subject.authorize(datasets, Ability.DOWNLOAD);
	}


	public static boolean registerForAuthentication(UserManageable userManager, User user, CredentialType credentials, boolean override) {
		if(override) {
			return userManager.updateUser(user.getId(), credentials);
		}
		return userManager.addUser(user.getId(), credentials);
	}
}
