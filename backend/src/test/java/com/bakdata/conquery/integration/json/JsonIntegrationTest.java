package com.bakdata.conquery.integration.json;

import java.io.File;

import jakarta.validation.Validator;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import io.dropwizard.jersey.validation.Validators;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@RequiredArgsConstructor
public class JsonIntegrationTest extends IntegrationTest.Simple {

	public static final Validator VALIDATOR = Validators.newValidator();

	private final ConqueryTestSpec testSpec;


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

}
