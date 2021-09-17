package com.bakdata.conquery.models.auth.apitoken;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.collect.ImmutableSet;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE) // Jackson
@Getter
@AllArgsConstructor
public class ApiTokenData implements Authorized, Owned {
	// TODO add serialization test

	/**
	 * The id is used to reference the token from outside the realm, i.e. the API.
	 */
	private final UUID id;
	/**
	 * The hash is the hashed API-token and not the hash of this object.
	 * It is only used internally in the realm to get a mapping back to the key of this object.
	 * This is used when a token is deleted:
	 *  - The api provides the token id (UUID) for the token that needs to be deleted
	 *  - The realm queries the storage for the {@link ApiTokenData} with that id
	 *  - The realm gets the token hash from the data
	 *  - The realm uses this token hash to delete the data from the store
	 */
	private final ApiTokenRealm.ApiTokenHash tokenHash;
	private final String name;
	private final UserId userId;
	private final LocalDate creationDate;
	private final LocalDate expirationDate;
	private final Set<Scopes> scopes;

	/**
	 * @implNote This is the only member that would be mutable otherwise.
	 */
	public Set<Scopes> getScopes() {
		return ImmutableSet.copyOf(scopes);
	}

	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@EqualsAndHashCode.Exclude
	@JsonIgnore
	private MetaStorage storage;


	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return AdminPermission.onDomain();
	}

	@Override
	public User getOwner() {
		return storage.getUser(userId);
	}

	/**
	 * Dynamic information about the token
	 */
	@Data
	public static class MetaData{
		@NotNull
		private final LocalDate lastUsed;
	}

	public boolean isCoveredByScopes(ConqueryPermission permission) {
		for (Scopes scope : scopes) {
			if (scope.isPermissionInScope(permission)) {
				return true;
			}
		}
		return false;
	}
}
