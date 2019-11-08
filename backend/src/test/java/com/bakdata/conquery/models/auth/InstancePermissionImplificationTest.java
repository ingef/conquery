package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;

public class InstancePermissionImplificationTest {
	
	private static final String DATASET1 = "dataset1";
	private static final String DATASET2 = "dataset2";
	

	@Test
	public void testEqual() {
		// Test equal Permissions
		Permission pStored = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingPrincipals() {
		// Test different user principals
		Permission pStored = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingAccesTypes() {
		// Test different access types
		Permission pStored = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.INSTANCE.instancePermission(
				Ability.DELETE.asSet(),
				new DatasetId(DATASET1));
		assert !pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingInstances() {
		// Test different Instances
		Permission pStored = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET2));
		assert !pStored.implies(pRequested);
	}
	
	@Test
	public void testMultipleAccessesProhibit() {
		// Test different Instances
		Permission pStored = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.INSTANCE.instancePermission(
				EnumSet.of(Ability.READ, Ability.DELETE),
				new DatasetId(DATASET1));
		// Should not imply, since one access is missing
		assert !pStored.implies(pRequested);
	}

	@Test
	public void testMultipleAccessesPermit() {
		// Test different Instances
		Permission pStored = DatasetPermission.INSTANCE.instancePermission(
				EnumSet.of(Ability.READ, Ability.DELETE),
				new DatasetId(DATASET1));
		Permission pRequested = DatasetPermission.INSTANCE.instancePermission(
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void permissionTypesFail() {
		Permission dPerm = DatasetPermission.INSTANCE.instancePermission(Ability.READ.asSet(), new DatasetId(DATASET1));
		Permission qPerm = QueryPermission.INSTANCE.instancePermission(Ability.READ.asSet(), new ManagedExecutionId(new DatasetId(DATASET1), UUID.randomUUID()));
		Permission sPerm = SuperPermission.INSTANCE.domainPermission();
		Permission aPerm = AdminPermission.INSTANCE.domainPermission();
		assertThat(dPerm.implies(qPerm)).isFalse();
		assertThat(dPerm.implies(sPerm)).isFalse();
		assertThat(aPerm.implies(sPerm)).isFalse();
	}
}
