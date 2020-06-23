package com.bakdata.conquery.models.auth;

import static com.bakdata.conquery.integration.common.IntegrationUtils.clearAuthStorage;
import static com.bakdata.conquery.integration.common.IntegrationUtils.importPermissionConstellation;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.PermissionToCheck;
import com.bakdata.conquery.integration.common.RequiredUser;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultimap;
import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.authz.Permission;

/**
 * This test specification loads a Role-User-Permission-constellation and test-data from a json file.
 * After the permission relevant data is populated in Conquery. The test-data contains
 * permission, that need to be checked , along with the expected users, that are permitted according to
 * the constellation. All User are than confronted with the permissions to check.
 *
 */
@CPSType(id="PERMISSION_TEST",base=ConqueryTestSpec.class)
@Getter @Setter
public class PermissionCheckTest extends ConqueryTestSpec  {

	@Valid
	private Role [] roles = new Role[0];
	@Valid @NotNull @JsonProperty("users")
	private RequiredUser [] rUsers;
	@Valid @NotNull
	private PermissionToCheck [] permissionsToCheck;
	
	@JsonIgnore
	private HashMultimap<ConqueryPermission, UserId> expectedPermitts = HashMultimap.create();
	
	@JsonIgnore
	private MasterMetaStorage storage = null;

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {
		storage = support.getStandaloneCommand().getMaster().getStorage();

		// Clear MasterStorage
		clearAuthStorage(storage, roles, rUsers);
		
		importPermissionConstellation(storage, roles, rUsers);
		
		// Load data that is compared to the calculated permissions
		for(PermissionToCheck permissionToCheck : permissionsToCheck) {
			ConqueryPermission permission = permissionToCheck.getPermission();
			expectedPermitts.putAll(permission, Arrays.asList(permissionToCheck.getPermitted()));
		}
	}
	
	@JsonIgnore
	public Map<? extends ConqueryPermission, ? extends Collection<UserId>> getExpected() {
		return expectedPermitts.asMap();
	}
	
	/**
	 * Checks the correctness of the following functions:
	 * {@link User#isPermitted(Permission)}
	 * , and indirectly:
	 * {@link Role#isPermitted(Permission)}
	 * @return
	 */
	@JsonIgnore
	public Map<ConqueryPermission, Collection<UserId>> getGrantedElementwiseCheck() {
		HashMultimap<ConqueryPermission, UserId> granted = HashMultimap.create();
		for(RequiredUser rUser: rUsers){
			User user = rUser.getUser();
			for(ConqueryPermission permission : expectedPermitts.keys()) {
				if(user.isPermitted(permission)){
					granted.put(permission, user.getId());
				}
			}
		}
		return granted.asMap();
	}
	
	@JsonIgnore
	public List<UserId> getExpectedAllGranted(){
		List<UserId> ret = new ArrayList<>();
		int countPermitts = expectedPermitts.keySet().size();
		for(User user : Arrays.asList(rUsers).stream().map(RequiredUser::getUser).map(User.class::cast).collect(Collectors.toList())) {
			if (Collections.frequency(expectedPermitts.values(), user.getId())== countPermitts) {
				ret.add(user.getId());
			}
		}
		return ret;
	}
	
	/**
	 * Checks the correctness of the following functions:
	 * {@link User#isPermittedAll(Collection)}
	 * , and indirectly:
	 * {@link User#isPermitted(List)},
	 * {@link Role#isPermitted(List)}
	 * @return
	 */
	@JsonIgnore
	public List<UserId> getGrantedListedCheck() {
		List<UserId> ret = new ArrayList<>();
		for(RequiredUser rUser: rUsers){
			User user = rUser.getUser();
			if(user.isPermittedAll(new ArrayList<Permission>(expectedPermitts.keys().elementSet()))) {
				ret.add(user.getId());
			}
		}
		return ret;
	}

	@Override
	public void executeTest(StandaloneSupport support) throws IOException {
		assertThat(getGrantedElementwiseCheck()).as("permissions individually checked on users").containsAllEntriesOf(getExpected());
		assertThat(getGrantedListedCheck()).as("list of permission checked on user").containsAnyElementsOf(getExpectedAllGranted());

		// Clear MasterStorage
		clearAuthStorage(storage, roles, rUsers);
	}
}
