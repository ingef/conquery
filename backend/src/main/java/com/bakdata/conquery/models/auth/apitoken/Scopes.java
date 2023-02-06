package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.models.auth.permissions.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Scopes to group permission types and restrict permissions of {@link ApiToken} (see {@link ApiTokenData}).
 */
@RequiredArgsConstructor
public enum Scopes {
	/**
	 * Allows to use the admin interface
 	 */
	ADMIN(Set.of(AdminPermission.DOMAIN)),
	/**
	 * Allows to access entities of a dataset such as the dataset in general and its concepts.
 	 */
	DATASET(Set.of(DatasetPermission.DOMAIN, ConceptPermission.DOMAIN)),
	/**
	 * Allows to create and use execution related entities such as Queries and {@link com.bakdata.conquery.models.forms.configs.FormConfig}s
	 */
	EXECUTIONS(Set.of(ExecutionPermission.DOMAIN, FormConfigPermission.DOMAIN, FormPermission.DOMAIN));

	@NonNull
	@JsonIgnore
	private final Set<String> scopeDomains;

	/**
	 * The permission is covered by the scope if all domains in the permission
	 * are covered by the scope's domains.
	 * @param permission the permission to test.
	 * @return True, if all domains are supported by the scope.
	 *
	 * @implNote At the moment we use only one domain per permission.
	 */
	boolean isPermissionInScope(@NonNull ConqueryPermission permission) {
		return scopeDomains.containsAll(permission.getDomains());
	}
}
