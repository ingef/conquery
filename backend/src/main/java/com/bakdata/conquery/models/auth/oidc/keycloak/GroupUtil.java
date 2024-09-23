package com.bakdata.conquery.models.auth.oidc.keycloak;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

public class GroupUtil {

	public static final String HIERARCHY_SEPARATOR = "/";

	/**
	 * Return direct and indirect group memberships (parent groups of provided userGroups).
	 *
	 * @param userGroups     The group which a user direct member of
	 * @param groupHierarchy The complete group hierarchy
	 * @return all group memberships
	 */
	@NotNull
	public static Set<KeycloakGroup> getAllUserGroups(Set<KeycloakGroup> userGroups, Set<KeycloakGroup> groupHierarchy) {
		Set<KeycloakGroup> allMemberships = Collections.emptySet();
		for (KeycloakGroup userGroup : userGroups) {
			allMemberships = Sets.union(allMemberships, GroupUtil.getParentGroups(userGroup, groupHierarchy));
		}
		return allMemberships;
	}

	/**
	 * Return the provided group and all parent groups in the provided hierarchy.
	 * <p>
	 * Using this hierarchy
	 * <pre>
	 * a
	 * ├─ aa
	 * ├─ ab
	 * ├─ ac
	 * b
	 * ├─ ba
	 * ├─ bb
	 * │  ├─ bba
	 * │  ├─ bb
	 * │  ├─ bbc
	 * ├─ bc
	 * c
	 * </pre>
	 * and providing the group
	 * <pre>bb</pre>
	 * would return
	 * <pre>[bb, b]</pre>
	 * <p>
	 * For more exampled see GroupUtilTest
	 *
	 * @param group     The group whose parents are collected
	 * @param hierarchy The group hierarchy in which to look for parents
	 * @return the provided group and all of its parents.
	 * @throws NoSuchElementException if the hierarchy is empty or the provided group is not
	 *                                contianed in the hierarchy.
	 */
	public static Set<KeycloakGroup> getParentGroups(KeycloakGroup group, Set<KeycloakGroup> hierarchy) {
		final ImmutableSet.Builder<KeycloakGroup> builder = ImmutableSet.builder();

		getParentGroups(group, hierarchy, builder);

		return builder.build();
	}

	private static void getParentGroups(KeycloakGroup group, Set<KeycloakGroup> hierarchy, ImmutableCollection.Builder<KeycloakGroup> builder) {
		if (hierarchy == null || hierarchy.isEmpty()) {
			throw new NoSuchElementException("Group '" + group.path() + "' cannot be found in the hierarchy");
		}

		boolean foundMatchingPath = false;

		for (KeycloakGroup groupParent : hierarchy) {
			if (group.equals(groupParent)) {
				builder.add(group);
				return;
			}

			if (group.path().startsWith(groupParent.path() + HIERARCHY_SEPARATOR)) {

				if (foundMatchingPath) {
					throw new IllegalStateException("Group '" + group.path() + "' fits into multiple paths of the group hierarchy");
				}
				foundMatchingPath = true;

				getParentGroups(group, groupParent.subGroups(), builder);
				builder.add(groupParent);
			}
		}

		if (!foundMatchingPath) {
			throw new NoSuchElementException("Group '" + group.path() + "' cannot be found in the hierarchy");
		}
	}
}
