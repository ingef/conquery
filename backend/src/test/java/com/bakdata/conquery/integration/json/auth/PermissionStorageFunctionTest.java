package com.bakdata.conquery.integration.json.auth;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
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
	private Role [] roles = new Role[0];
	@Valid @NotNull @JsonProperty("users")
	private RequiredUser [] rUsers;
	
	@JsonIgnore
	private MasterMetaStorage storage;
	
	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

		storage = support.getStandaloneCommand().getMaster().getStorage();

		// Clear MasterStorage
		//clearAuthStorage(storage);
	}
	
	public void addMandators() throws JSONException {
		for(PermissionOwner<RoleId> role: roles) {
			storage.addRole((Role)role);
		}
	}
	
	public void addUsers() throws JSONException {
		for(RequiredUser rUser: rUsers) {
			User user = (User)rUser.getUser();
			storage.addUser(user);
		}
	}
	
	public void updateUsers() throws JSONException {
		for(RequiredUser rUser: rUsers) {
			User user = storage.getUser(rUser.getUser().getId());
			RoleId [] rolesInjected = rUser.getRolesInjected();
			
			for(RoleId mandatorId : rolesInjected) {
				user.addRole(storage,storage.getRole(mandatorId));
			}
		}
	}


	public void removeUser(User user) {
		storage.removeUser(user.getId());
	}

	public Collection<Role> getMandatorsStored(){
		return storage.getAllRoles();
	}
	
	public Collection<Role> getMandatorsExpected(){
		return Arrays.asList(roles)
				.stream()
				.map(Role.class::cast)
				.collect(Collectors.toList());
	}
	
	public void removeMandator(Role mandator) {
		storage.removeRole(mandator.getId());
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
					for(RoleId id : rUser.getRolesInjected()){
						user.addRoleLocal(storage.getRole(id));
					}
					return user;
				}).collect(Collectors.toList());
		
	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		// tests adding of mandators
		addMandators();
		assertThat(getMandatorsStored())
			.containsAll(getMandatorsExpected());
		
		// tests adding of users
		addUsers();
		assertThat(getUsersStored())
			.containsAll(getUsersExpected());
		
		// test updating of roles of users
		updateUsers();
		assertThat(getUsersStored())
			.containsAll(getUsersUpdatedExpected());
		
		// tests removing of users
		List<User> users = Arrays.asList(getRUsers()).stream().map(RequiredUser::getUser).map(User.class::cast).collect(Collectors.toList());
		Iterator<User> userIt = users.iterator();
		while(userIt.hasNext()) {
			User user = userIt.next();
			removeUser(user);
			userIt.remove();
			assertThat(getUsersStored()).doesNotContain(user);
		}
		
		// tests removing of mandators
		List<Role> mandators = Arrays.asList(getRoles()).stream().map(Role.class::cast).collect(Collectors.toList());
		Iterator<Role> mandatorIt = mandators.iterator();
		while(mandatorIt.hasNext()) {
			Role mandator = mandatorIt.next();
			removeMandator(mandator);
			mandatorIt.remove();
			assertThat(getMandatorsStored()).doesNotContain(mandator);
		}
	}
}
