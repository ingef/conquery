package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

public class PermissionCreationTest {
	@Test
	public void createPermissionLegalAbility() {
		assertThat(new DatasetPermission(Ability.READ.asSet(), new DatasetId("test"))).isInstanceOf(DatasetPermission.class);
	}
	
	@Test
	public void createPermissionIllegalAbility() {
		ConqueryPermission perm = null ;
		try {
			// This should fail because the ability is not allowed for a DatasetPermission
			perm = new DatasetPermission(Ability.DUMMY_ABILITY.asSet(), new DatasetId("test"));
		}catch (Exception e) {			
			assertThat(e).isInstanceOf(IllegalStateException.class);
		}
		if(perm != null) {			
			fail();
		}
	}

}
