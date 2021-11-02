package com.bakdata.conquery.integration.json;

import java.io.IOException;

import javax.validation.Validator;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectReader;
import io.dropwizard.jersey.validation.Validators;
import io.github.classgraph.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class JsonIntegrationTest extends IntegrationTest.Simple {

	public static final ObjectReader TEST_SPEC_READER = Jackson.MAPPER.readerFor(ConqueryTestSpec.class);
	public static final Validator VALIDATOR = Validators.newValidator();
	@Getter
	private final ConqueryTestSpec testSpec;

	public static JsonIntegrationTest read(Resource resource) throws IOException {
		final String path = resource.getPath();
		final String root = path.substring(0, path.lastIndexOf('/') + 1);

		ConqueryTestSpec spec = TEST_SPEC_READER.with(new InjectableValues.Std().addValue("root", root)).readValue(resource.open());

		return new JsonIntegrationTest(spec);
	}

	@Override
	public void overrideConfig(ConqueryConfig conf) {
		testSpec.overrideConfig(conf);
	}

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		ValidatorHelper.failOnError(log, VALIDATOR.validate(testSpec));

		testSpec.importRequiredData(conquery);

		//ensure the metadata is collected
		conquery.getNamespace().sendToAll(new UpdateMatchingStatsMessage());

		conquery.waitUntilWorkDone();

		testSpec.executeTest(conquery);
	}

	public static ConqueryTestSpec readJson(DatasetId dataset, String json) throws IOException {
		return readJson(dataset, json, TEST_SPEC_READER);
	}

	public static ConqueryTestSpec readJson(Dataset dataset, String json) throws IOException {
		return readJson(dataset.getId(), json, dataset.injectIntoNew(TEST_SPEC_READER));
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
