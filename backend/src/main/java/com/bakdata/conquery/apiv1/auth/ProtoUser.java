package com.bakdata.conquery.apiv1.auth;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.UserManageable;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Container class for holding information about initial users.
 */
@Getter
@Builder
public class ProtoUser {

	private String label;
	@NotEmpty
	@Builder.Default
	private String name = "DefaultUserName";

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
	private List<CredentialType> credentials = Collections.emptyList();
	
	@JsonIgnore
	// Let this be ignored by the builder
	private User user = null;

	public User getUser() {
		if(user != null) {
			return user;
		}
		if (label == null) {
			label = name;
		}
		user = new User(name, label);
		return user;
	}

	public void registerForAuthorization(MasterMetaStorage storage, boolean override) {
		User user = this.getUser();
		if(override) {			
			storage.updateUser(user);
		} else {
			// Should throw an exception, if the user already existed
			storage.addUser(user);
		}
		for (String sPermission : permissions) {
			user.addPermission(storage, new WildcardPermission(sPermission));
		}
	}
	
	public boolean registerForAuthentication(UserManageable userManager, boolean override) {
		if(override) {			
			return userManager.updateUser(getUser(), credentials);
		}
		return userManager.addUser(getUser(), credentials);
	}
}
