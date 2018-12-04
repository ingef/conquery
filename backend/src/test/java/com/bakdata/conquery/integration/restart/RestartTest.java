package com.bakdata.conquery.integration.restart;

import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.integration.ConqueryTestSpec;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;

import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestartTest {

	@Test
	public void testRestartingDatabase() throws Exception {
		//init
		IntegrationTest.reduceLogging();
		TestConquery testConquery = new TestConquery();
		try {
			//starts the server
			testConquery.beforeAll(null);
			
			//read test sepcification
			String testJson = In.file("tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
	
			Validator validator = Validators.newValidator();
			DatasetId dataset;
			ConqueryTestSpec test;
			
			
			try(StandaloneSupport conquery = testConquery.getSupport()) {
				dataset = conquery.getDataset().getId();
				
				test = IntegrationTest.readTest(dataset, testJson);
				ValidatorHelper.failOnError(log, validator.validate(test));
				
				test.importRequiredData(conquery);
		
				conquery.waitUntilWorkDone();
		
				test.executeTest(conquery);
			}
			
			//stop dropwizard directly so COnquerySupport does not delete the tmp directory
			testConquery.getDropwizard().after();
			//restart
			testConquery.beforeAll(null);
			
			try(StandaloneSupport conquery = testConquery.openDataset(dataset)) {
				test.executeTest(conquery);
				
				
			}
		}
		finally {
			testConquery.afterAll(null);
		}
	}
}
