package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

public class PermissionCreationTest {
	@Test
	public void createPermissionLegalAbility() {
		assertThat(DatasetPermission.INSTANCE.instancePermission(Ability.READ.asSet(), new DatasetId("test"))).isInstanceOf(Permission.class);
	}
	
	@Test
	public void createPermissionIllegalAbility() {
		Permission perm = null ;
		try {
			// This should fail because the ability is not allowed for a DatasetPermission
			perm = DatasetPermission.INSTANCE.instancePermission(Ability.SHARE.asSet(), new DatasetId("test"));
		}catch (Exception e) {			
			assertThat(e).isInstanceOf(IllegalArgumentException.class);
		}
		assertThat(perm).isNull();
	}

}
