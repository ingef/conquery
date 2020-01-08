package com.bakdata.conquery.models.auth;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;

@Slf4j
public class BasicAuthRealm extends ConqueryRealm {
	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = UsernamePasswordToken.class;
	private final Validator validator;
	private IdentifiableStore<User> authUser;
	private IdentifiableStore<Role> authRole;
	private IdentifiableStore<Group> authGroup;

	@Getter
	private final Environment usersEnvironment;

	@Getter
	private final Environment rolesEnvironment;

	@Getter
	private final Environment groupsEnvironment;
	 
	public BasicAuthRealm(StorageConfig config, Validator validator) {
		this.setAuthenticationTokenClass(TOKEN_CLASS);
		this.validator = validator;


		usersEnvironment = Environments.newInstance(
				new File(config.getDirectory(), "users"),
				config.getXodus().createConfig()
		);

		rolesEnvironment = Environments.newInstance(
				new File(config.getDirectory(), "roles"),
				config.getXodus().createConfig()
		);

		groupsEnvironment = Environments.newInstance(
			new File(config.getDirectory(), "groups"),
			config.getXodus().createConfig()
			);
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if(!(TOKEN_CLASS.isAssignableFrom(token.getClass()))) {
			// Incompatible token
			return null;
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void addRole(Role role) {
		try {
			ValidatorHelper.failOnError(log, validator.validate(role));
			log.trace("New role:\tLabel: {}\tName: {}\tId: {} ", role.getLabel(), role.getName(), role.getId());
			authRole.add(role);
		}
		catch (JSONException e) {
			throw new IllegalArgumentException("Validation or storing failed for " + role, e);
		}
	}

	@Override
	void addRoles(List<Role> roles) {
		for (Role role : roles) {
			try {
				addRole(role);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add Role: %s", role), e);
			}
		}
	}

	@Override
	void deleteRole(RoleId mandatorId) {
		// TODO Auto-generated method stub

	}

	@Override
	List<Role> getAllRoles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<User> getUsers(Role role) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	List<Group> getGroups(Role role) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void createPermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission) {
		// TODO Auto-generated method stub

	}

	@Override
	void deletePermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission) {
		// TODO Auto-generated method stub

	}

	@Override
	List<User> getAllUsers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void deleteUser(UserId userId) {
		// TODO Auto-generated method stub

	}

	@Override
	void addUser(User user) {
		// TODO Auto-generated method stub

	}

	@Override
	void addUsers(List<User> users) {
		// TODO Auto-generated method stub

	}

	@Override
	Collection<Group> getAllGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void addGroup(Group group) {
		// TODO Auto-generated method stub

	}

	@Override
	void addGroups(List<Group> groups) {
		// TODO Auto-generated method stub

	}

	@Override
	void addUserToGroup(GroupId groupId, UserId userId) {
		// TODO Auto-generated method stub

	}

	@Override
	void deleteUserFromGroup(GroupId groupId, UserId userId) {
		// TODO Auto-generated method stub

	}

	@Override
	void removeGroup(GroupId groupId) {
		// TODO Auto-generated method stub

	}

	@Override
	void deleteRoleFrom(PermissionOwnerId<?> ownerId, RoleId roleId) {
		// TODO Auto-generated method stub

	}

	@Override
	void addRoleTo(PermissionOwnerId<?> ownerId, RoleId roleId) {
		// TODO Auto-generated method stub

	}

}
