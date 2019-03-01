package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validator;

import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;

import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestartTest implements ProgrammaticIntegrationTest {
	
	private Mandator mandator = new Mandator("99999998", "MANDATOR_LABEL");
	private User user = new User("user@test.email", "USER_LABEL");

	@Override
	public void execute(TestConquery testConquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		Validator validator = Validators.newValidator();
		DatasetId dataset;
		ConqueryTestSpec test;
		
		
		try(StandaloneSupport conquery = testConquery.getSupport()) {
			dataset = conquery.getDataset().getId();
			
			test = JsonIntegrationTest.readJson(dataset, testJson);
			ValidatorHelper.failOnError(log, validator.validate(test));
			
			test.importRequiredData(conquery);
	
			test.executeTest(conquery);
			
			// Auth testing
			MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
			storage.addMandator(mandator);
			
			storage.addUser(user);
			user.addMandator(storage, mandator);
		}
		
		//stop dropwizard directly so COnquerySupport does not delete the tmp directory
		testConquery.getDropwizard().after();
		//restart
		testConquery.beforeAll(testConquery.getBeforeAllContext());
		
		try(StandaloneSupport conquery = testConquery.openDataset(dataset)) {
			test.executeTest(conquery);
			

			MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
			User userStored = storage.getUser(user.getId());
			Mandator mandatorStored = storage.getMandator(mandator.getId());
			Mandator userRefMand = userStored.getRoles().iterator().next();
			assertThat(mandatorStored).isSameAs(userRefMand);
			
		}
		
		
	}
}
