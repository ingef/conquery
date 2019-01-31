package com.bakdata.conquery.integration.tests;

import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;

import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestartTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(TestConquery testConquery) throws Exception {
		//read test sepcification
		String testJson = In.file("tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		Validator validator = Validators.newValidator();
		DatasetId dataset;
		ConqueryTestSpec test;
		
		
		try(StandaloneSupport conquery = testConquery.getSupport()) {
			dataset = conquery.getDataset().getId();
			
			test = JsonIntegrationTest.readJson(dataset, testJson);
			ValidatorHelper.failOnError(log, validator.validate(test));
			
			test.importRequiredData(conquery);
	
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
}
