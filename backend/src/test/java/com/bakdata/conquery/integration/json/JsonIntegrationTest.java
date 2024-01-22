package com.bakdata.conquery.integration.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.validation.Validator;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.ObjectReader;
import io.dropwizard.jersey.validation.Validators;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Getter
@Slf4j
@RequiredArgsConstructor
public class JsonIntegrationTest extends IntegrationTest.Simple {

	private final ConqueryTestSpec testSpec;
	public static final ObjectReader TEST_SPEC_READER = Jackson.MAPPER.readerFor(ConqueryTestSpec.class);
	public static final Validator VALIDATOR = Validators.newValidator();

	public JsonIntegrationTest(InputStream in) throws IOException {
		this.testSpec = TEST_SPEC_READER.readValue(in.readAllBytes());
		in.close();
	}

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		ValidatorHelper.failOnError(log, VALIDATOR.validate(testSpec));
		testSpec.importRequiredData(conquery);
		testSpec.executeTest(conquery);
	}

	@Override
	public ConqueryConfig overrideConfig(final ConqueryConfig conf, final File workDir) {
		return getTestSpec().overrideConfig(conf);
	}

	public static <T extends ConqueryTestSpec> T readJson(DatasetId dataset, String json) throws IOException {
		return readJson(dataset, json, TEST_SPEC_READER);
	}

	public static <T extends ConqueryTestSpec> T readJson(Dataset dataset, String json) throws IOException {
		return readJson(dataset.getId(), json, dataset.injectIntoNew(TEST_SPEC_READER));
	}

	private static <T extends ConqueryTestSpec> T readJson(DatasetId dataset, String json, ObjectReader jsonReader) throws IOException {
		json = StringUtils.replace(
				json,
				"${dataset}",
				dataset.toString()
		);

		return jsonReader.readValue(json);
	}

}
