package com.bakdata.conquery.integration.restart;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.integration.ConqueryTestSpec;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;

import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestartTest {

	public static final String LABEL = "TEST_LABEL";
	public static Mandator mandator = new Mandator("999999998", LABEL);
	public final static MandatorId MANDATOR_ID = mandator.getId();
	public static User user = new User("superUser", LABEL);
	public final static UserId USER_ID = user.getId();
	
	@Test
	public void testRestartingDatabase() throws Exception {
		//init
		IntegrationTest.reduceLogging();
		TestConquery testConquery = new TestConquery();
		try {
			//starts the server
			testConquery.beforeAll(null);
			
			//read test sepcification
			String testJson = In.file("tests/query/VALIDITY_DATE_QUERY/VALIDITY_DATE_QUERY_Query.test.json").withUTF8().readAll();
	
			Validator validator = Validators.newValidator();
			DatasetId dataset;
			ConqueryTestSpec test;
			
			
			try(StandaloneSupport conquery = testConquery.getSupport()) {
				dataset = conquery.getDataset().getId();
				
				// Query testing
				test = IntegrationTest.readTest(dataset, testJson);
				ValidatorHelper.failOnError(log, validator.validate(test));
				
				test.importRequiredData(conquery);
		
				test.executeTest(conquery);
				
				// Auth storage testing
				MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
				storage.addMandator(mandator);
				
				user.addMandatorLocal(mandator);
				storage.addUser(user);
			}
			
			//stop dropwizard directly so COnquerySupport does not delete the tmp directory
			testConquery.getDropwizard().after();
			//restart
			testConquery.beforeAll(null);
			
			try(StandaloneSupport conquery = testConquery.openDataset(dataset)) {
				test.executeTest(conquery);
				MasterMetaStorage storage = conquery.getStandaloneCommand().getMaster().getStorage();
				assertThat(storage.getMandator(MANDATOR_ID)).isEqualTo(mandator);
				assertThat(storage.getUser(USER_ID)).isEqualTo(user);
			}
		}
		finally {
			testConquery.afterAll(null);
		}
	}
}
