package com.bakdata.conquery.apiv1.auth;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Factory class to create configured initial roles.

 */
@Getter
@Builder
public class ProtoRole {

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

	public Role createOrOverwriteRole(@NonNull MetaStorage storage) {
		label = Objects.requireNonNullElse(label, name);


		Role role = new Role(name, label, storage);

		storage.updateRole(role);

		for (String permission : permissions) {
			role.addPermission(new WildcardPermission(permission));
		}

		return role;
	}

	@JsonIgnore
	public RoleId createId() {
		return new RoleId(name);
	}
}
