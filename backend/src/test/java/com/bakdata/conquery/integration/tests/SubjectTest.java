package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class SubjectTest implements ProgrammaticIntegrationTest, IntegrationTest.Simple {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
		
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		
		// setup mandator
		Mandator mandator1 = new Mandator("company", "company");
		storage.addMandator(mandator1);
		
		DatasetPermission datasetPermission1 = new DatasetPermission(mandator1.getId(), Ability.READ.asSet(), dataset1.getId());
		assertThat(mandator1.addPermission(storage, datasetPermission1).getId()).isEqualTo(datasetPermission1.getId());
		DatasetPermission datasetPermission2 = new DatasetPermission(mandator1.getId(), Ability.DELETE.asSet(), dataset1.getId());
		assertThat(mandator1.addPermission(storage, datasetPermission2).getId()).isNotEqualTo(datasetPermission2.getId());
		
		// setup user
		User user1  = new User("user", "user");
		storage.addUser(user1);
		
		user1.addMandator(storage, mandator1);
		user1.addMandator(storage, mandator1);
		assertThat(user1.getRoles()).containsExactlyInAnyOrder(mandator1);
	}

}
