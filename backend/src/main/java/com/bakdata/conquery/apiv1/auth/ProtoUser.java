package com.bakdata.conquery.apiv1.auth;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory class to create configured initial users.
 */
@Getter
@Builder
@Slf4j
public class ProtoUser {

	@NotEmpty
	private final String name;
	private final Set<String> roles;
	private String label;
	/**
	 * String permissions in the form of
	 * {@link org.apache.shiro.authz.permission.WildcardPermission}, that the user
	 * should hold after initialization.
	 */
	@Builder.Default
	@NotNull
	private Set<String> permissions = Collections.emptySet();

	/**
	 * These are passed to realms that are able to manage users (implementing
	 * {@link UserManageable}, such as {@link LocalAuthenticationRealm}).
	 */
	@Builder.Default
	@Valid
	private CredentialType credential = null;

	public User createOrOverwriteUser(@NonNull MetaStorage storage) {
		if (label == null) {
			label = name;
		}

		final User user = new User(name, label);
		user.setMetaStorage(storage);
		storage.updateUser(user);

		if (roles != null){
			for (String roleId : roles) {
				final Role role = storage.getRole(new RoleId(roleId));

				if(role == null){
					log.warn("Unknown Role[{}] for {}", roleId, this);
					continue;
				}

				user.addRole(role);
			}
		}


		for (String sPermission : permissions) {
			user.addPermission(new WildcardPermission(sPermission));
		}


		return user;
	}

	@JsonIgnore
	public UserId createId() {
		return new UserId(name);
	}
}
