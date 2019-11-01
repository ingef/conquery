package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;

public class SetTest {
	
	@Test
	public void testSet() {
		Set<ConqueryPermission> permissions = new HashSet<>();
		
		DatasetPermission perm = new DatasetPermission(Ability.DELETE.asSet(), new DatasetId("testDataset"));
		DatasetPermission perm1 = new DatasetPermission(Ability.DELETE.asSet(), new DatasetId("testDataset"));
		permissions.add(perm);
		assertThat(permissions.remove(perm1)).isTrue();
	}

}
