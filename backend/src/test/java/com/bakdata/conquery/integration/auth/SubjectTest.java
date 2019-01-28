package com.bakdata.conquery.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import com.bakdata.conquery.integration.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.support.StandaloneSupport;


@CPSType(id="SUBJECT_TEST",base=ConqueryTestSpec.class)
public class SubjectTest extends ConqueryTestSpec {

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		MasterMetaStorage storage = support.getStandaloneCommand().getMaster().getStorage();
		
		Dataset dataset1 = new Dataset();
		dataset1.setLabel("dataset1");
		
		// setup mandator
		Mandator mandator1 = new Mandator(new SinglePrincipalCollection(new MandatorId("company")));
		mandator1.setLabel("company");
		storage.addMandator(mandator1);
		
		DatasetPermission datasetPermission1 = new DatasetPermission(mandator1.getId(), EnumSet.of(Ability.READ), dataset1.getId());
		assertThat(mandator1.addPermission(datasetPermission1).getId()).isEqualTo(datasetPermission1.getId());
		DatasetPermission datasetPermission2 = new DatasetPermission(mandator1.getId(), EnumSet.of(Ability.DELETE), dataset1.getId());
		assertThat(mandator1.addPermission(datasetPermission2).getId()).isNotEqualTo(datasetPermission2.getId());
		
		// setup user
		User user1  = new User(new SinglePrincipalCollection(new UserId("user")));
		user1.setLabel("user");
		storage.addUser(user1);
		
		user1.addMandator(mandator1);
		user1.addMandator(mandator1);
		assertThat(user1.getRoles()).containsExactlyInAnyOrder(mandator1);
	}

	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {

	}

}
