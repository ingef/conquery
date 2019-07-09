package com.bakdata.conquery.models.auth;

import java.util.EnumSet;

import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

public class InstancePermissionImplificationTest {
	
	private static final String USERPROP1 = "user1@test";
	private static final String USERPROP2 = "user2@test";
	
	private static final String DATASET1 = "dataset1";
	private static final String DATASET2 = "dataset2";
	

	@Test
	public void testEqual() {
		// Test equal Permissions
		Permission pStored = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingPrincipals() {
		// Test different user principals
		Permission pStored = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = new DatasetPermission(
				new UserId(USERPROP2),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingAccesTypes() {
		// Test different access types
		Permission pStored = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.DELETE.asSet(),
				new DatasetId(DATASET1));
		assert !pStored.implies(pRequested);
	}
	
	@Test
	public void testDivergingInstances() {
		// Test different Instances
		Permission pStored = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET2));
		assert !pStored.implies(pRequested);
	}
	
	@Test
	public void testMultipleAccessesProhibit() {
		// Test different Instances
		Permission pStored = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		Permission pRequested = new DatasetPermission(
				new UserId(USERPROP1),
				EnumSet.of(Ability.READ, Ability.DELETE),
				new DatasetId(DATASET1));
		// Should not imply, since one access is missing
		assert !pStored.implies(pRequested);
	}

	@Test
	public void testMultipleAccessesPermit() {
		// Test different Instances
		Permission pStored = new DatasetPermission(
				new UserId(USERPROP1),
				EnumSet.of(Ability.READ, Ability.DELETE),
				new DatasetId(DATASET1));
		Permission pRequested = new DatasetPermission(
				new UserId(USERPROP1),
				Ability.READ.asSet(),
				new DatasetId(DATASET1));
		assert pStored.implies(pRequested);
	}
}
