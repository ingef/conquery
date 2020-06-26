package com.bakdata.conquery.integration.json;

import java.io.IOException;

import javax.validation.Validator;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.jobs.UpdateMatchingStats;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import io.dropwizard.jersey.validation.Validators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j @RequiredArgsConstructor
public class JsonIntegrationTest extends IntegrationTest.Simple {
	
	public static final ObjectReader TEST_SPEC_READER = Jackson.MAPPER.readerFor(ConqueryTestSpec.class);
	public static final Validator VALIDATOR = Validators.newValidator();
	private final JsonNode node;
	
	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		ConqueryTestSpec test = readJson(conquery.getDataset(), Jackson.MAPPER.writeValueAsString(node));

		ValidatorHelper.failOnError(log, VALIDATOR.validate(test));

		test.importRequiredData(conquery);

		//ensure the metadata is collected
		for(SlaveCommand slave : conquery.getSlaves()) {
			slave.getWorkers().getWorkers().values().forEach(worker -> {
				worker.getJobManager().addSlowJob(new UpdateMatchingStats(worker));
			});
		}
		
		conquery.waitUntilWorkDone();

		test.executeTest(conquery);
	}
	
	public static ConqueryTestSpec readJson(DatasetId dataset, String json) throws IOException {
		return readJson(dataset, json, TEST_SPEC_READER);
	}
	
	public static ConqueryTestSpec readJson(Dataset dataset, String json) throws IOException {
		return readJson(dataset.getId(), json, dataset.injectInto(TEST_SPEC_READER));
	}
	
	private static ConqueryTestSpec readJson(DatasetId dataset, String json, ObjectReader jsonReader) throws IOException {
		json = StringUtils.replace(
			json,
			"${dataset}",
			dataset.toString()
		);
		
		return jsonReader.readValue(json);
	}
}
