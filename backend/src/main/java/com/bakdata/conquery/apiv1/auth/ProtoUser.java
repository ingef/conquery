package com.bakdata.conquery.apiv1.auth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Container class for holding information about initial users.
 */
@Getter
@Builder
public class ProtoUser {

	private String label;
	@NotEmpty
	private final String name;

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
	@NotNull
	@Valid
	private List<CredentialType> credentials = Collections.emptyList();

	public User createOrOverwriteUser(@NonNull MetaStorage storage) {
		if (label == null) {
			label = name;
		}
		User user = new User(name, label, storage);
		storage.updateUser(user);
		for (String sPermission : permissions) {
			user.addPermission(new WildcardPermission(sPermission));
		}
		return user;
	}

	@org.jetbrains.annotations.NotNull
	public UserId getId() {
		return new UserId(name);
	}

	public static boolean registerForAuthentication(UserManageable userManager, User user, List<CredentialType> credentials, boolean override) {
		if(override) {			
			return userManager.updateUser(user, credentials);
		}
		return userManager.addUser(user, credentials);
	}
}
