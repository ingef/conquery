package com.bakdata.conquery.integration.json.auth;

import static com.bakdata.conquery.integration.common.IntegrationUtils.clearAuthStorage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.RequiredUser;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * This testspec checks if permission related data can be added, updated, deleted
 * using the provided store.
 */
@CPSType(id="PERMISSION_STORAGE_FUNCTION_TEST",base=ConqueryTestSpec.class)
@Getter @Setter
public class PermissionStorageFunctionTest extends ConqueryTestSpec {

	@Valid
	private Mandator [] roles = new Mandator[0];
	@Valid @NotNull @JsonProperty("users")
	private RequiredUser [] rUsers;
	@Valid @NotNull
	private ConqueryPermission [] permissions;
	
	@JsonIgnore
	private MasterMetaStorage storage;
	
	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

		storage = support.getStandaloneCommand().getMaster().getStorage();

		// Clear MasterStorage
		clearAuthStorage(storage);
	}
	
	public void addMandators() throws JSONException {
		for(PermissionOwner<MandatorId> role: roles) {
			storage.addMandator((Mandator)role);
		}
	}
	
	public void addUsers() throws JSONException {
		for(RequiredUser rUser: rUsers) {
			User user = (User)rUser.getUser();
			storage.addUser(user);
		}
	}
	
	public void addPermissions() throws JSONException {
		for(ConqueryPermission permission: permissions) {
			permission.getId();
			PermissionOwnerId<?> ownerId = permission.getOwnerId();
			PermissionOwner<?> owner =null;
			if(ownerId instanceof UserId) {
				owner = storage.getUser((UserId) ownerId);
			} else if(ownerId instanceof MandatorId) {
				owner = storage.getMandator((MandatorId) ownerId);
			}
			
			owner.addPermission(permission);
		}
	}
	
	public void removePermission(ConqueryPermission permission) throws JSONException {
		storage.removePermission(permission.getId());
	}
	
	public void updateUsers() throws JSONException {
		for(RequiredUser rUser: rUsers) {
			User user = storage.getUser(rUser.getUser().getId());
			MandatorId [] rolesInjected = rUser.getRolesInjected();
			
			for(MandatorId mandatorId : rolesInjected) {
				user.addRole(storage.getMandator(mandatorId));
			}
			storage.updateUser(user);
		}
	}


	public void removeUser(User user) {
		storage.removeUser(user.getId());
	}

	public Collection<Mandator> getMandatorsStored(){
		return storage.getAllMandators();
	}
	
	public Collection<Mandator> getMandatorsExpected(){
		return Arrays.asList(roles)
				.stream()
				.map(Mandator.class::cast)
				.collect(Collectors.toList());
	}
	
	public void removeMandator(Mandator mandator) {
		storage.removeMandator(mandator.getId());
	}
	
	public Collection<User> getUsersStored(){
		return storage.getAllUsers();
	}
	
	public Collection<User> getUsersExpected(){
		return Arrays.asList(rUsers)
				.stream()
				.map(RequiredUser::getUser)
				.map(User.class::cast)
				.collect(Collectors.toList());
	}
	
	public Collection<User> getUsersUpdatedExpected(){
		return Arrays.asList(rUsers)
				.stream()
				.map(rUser ->{
					User user = (User)rUser.getUser();
					user.addRoles((List<Mandator>)Arrays.asList(rUser.getRolesInjected())
						.stream()
						.map(MandatorId.class::cast)
						.map(mId -> storage.getMandator(mId))
						.collect(Collectors.toList()));
					return user;
				}).map(User.class::cast).collect(Collectors.toList());
		
	}
	
	public Collection<ConqueryPermission> getPermissionsStored(){
		return storage.getAllPermissions();
	}
	
	public Collection<ConqueryPermission> getPermissionsExpected(){
		return Arrays.asList(permissions);
	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		// tests adding of mandators
		addMandators();
		assertThat(getMandatorsStored())
			.containsExactlyInAnyOrderElementsOf(getMandatorsExpected());
		
		// tests adding of users
		addUsers();
		assertThat(getUsersStored())
			.containsExactlyInAnyOrderElementsOf(getUsersExpected());
		
		// test updating of roles of users
		updateUsers();
		assertThat(getUsersStored())
			.containsExactlyInAnyOrderElementsOf(getUsersUpdatedExpected());
		
		// tests adding of permissions
		addPermissions();
		assertThat(getPermissionsStored())
			.containsExactlyInAnyOrderElementsOf(getPermissionsExpected());
		
		// tests removing of permissions
		List<ConqueryPermission> permissions = new ArrayList<>(Arrays.asList(getPermissions()));
		Iterator<ConqueryPermission> perIt = permissions.iterator();
		while(perIt.hasNext()) {
			ConqueryPermission cp = perIt.next();
			removePermission(cp);
			perIt.remove();
			assertThat(getPermissionsStored()).containsExactlyInAnyOrderElementsOf(permissions);
		}

		// tests removing of users
		List<User> users = Arrays.asList(getRUsers()).stream().map(RequiredUser::getUser).map(User.class::cast).collect(Collectors.toList());
		Iterator<User> userIt = users.iterator();
		while(userIt.hasNext()) {
			User user = userIt.next();
			removeUser(user);
			userIt.remove();
			assertThat(getUsersStored()).containsExactlyInAnyOrderElementsOf(users);
		}
		
		// tests removing of mandators
		List<Mandator> mandators = Arrays.asList(getRoles()).stream().map(Mandator.class::cast).collect(Collectors.toList());
		Iterator<Mandator> mandatorIt = mandators.iterator();
		while(mandatorIt.hasNext()) {
			Mandator mandator = mandatorIt.next();
			removeMandator(mandator);
			mandatorIt.remove();
			assertThat(getMandatorsStored()).containsExactlyInAnyOrderElementsOf(mandators);
		}
	}
}
