package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.UUID;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.Test;

public class InstancePermissionImplificationTest {
	
	private static final String DATASET1 = "dataset1";
	private static final String DATASET2 = "dataset2";
	

	@Test
	public void testEqual() {
		// Test equal Permissions
		Permission pStored = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingPrincipals() {
		// Test different user principals
		Permission pStored = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingAccesTypes() {
		// Test different access types
		Permission pStored = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.onInstance(
			Ability.DOWNLOAD.asSet(),
				new DatasetId(DATASET1));
		assert !pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingInstances() {
		// Test different Instances
		Permission pStored = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET2));
		assert !pStored.implies(pRequested);
	}
	
	@Test
	public void testMultipleAccessesProhibit() {
		// Test different Instances
		Permission pStored = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.onInstance(
			EnumSet.of(Ability.READ, Ability.DOWNLOAD),
				new DatasetId(DATASET1));
		// Should not imply, since one access is missing
		assert !pStored.implies(pRequested);
	}

	@Test
	public void testMultipleAccessesPermit() {
		// Test different Instances
		Permission pStored = DatasetPermission.onInstance(
			EnumSet.of(Ability.READ, Ability.DOWNLOAD),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.onInstance(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void permissionTypesFail() {
		Permission dPerm = DatasetPermission.onInstance(Ability.READ.asSet(), new DatasetId(DATASET1));
		Permission qPerm = ExecutionPermission.onInstance(Ability.READ.asSet(), new ManagedExecutionId(new DatasetId(DATASET1), UUID.randomUUID()));
		Permission sPerm = SuperPermission.onDomain();
		Permission aPerm = AdminPermission.onDomain();
		assertThat(dPerm.implies(qPerm)).isFalse();
		assertThat(dPerm.implies(sPerm)).isFalse();
		assertThat(aPerm.implies(sPerm)).isFalse();
	}
}
