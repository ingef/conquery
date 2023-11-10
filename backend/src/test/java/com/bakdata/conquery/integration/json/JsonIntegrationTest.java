package com.bakdata.conquery.integration.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.validation.Validator;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.integration.sql.dialect.TestSqlDialect;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
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
public abstract class JsonIntegrationTest extends IntegrationTest.Simple {

	public static final ObjectReader TEST_SPEC_READER = Jackson.MAPPER.readerFor(ConqueryTestSpec.class);
	public static final Validator VALIDATOR = Validators.newValidator();

	public abstract void execute(StandaloneSupport conquery) throws Exception;

	public abstract ConqueryTestSpec getTestSpec();

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

	@Override
	public ConqueryConfig overrideConfig(final ConqueryConfig conf, final File workDir) {
		return getTestSpec().overrideConfig(conf);
	}

	@Getter
	public static class Distributed extends JsonIntegrationTest {

		private final ConqueryTestSpec testSpec;

		public Distributed(InputStream in) throws IOException {
			this.testSpec = TEST_SPEC_READER.readValue(in.readAllBytes());
			in.close();
		}

		@Override
		public void execute(StandaloneSupport conquery) throws Exception {
			ValidatorHelper.failOnError(log, VALIDATOR.validate(testSpec));

			testSpec.importRequiredData(conquery);

			//ensure the metadata is collected
			DistributedNamespace namespace = (DistributedNamespace) conquery.getNamespace();
			namespace.getWorkerHandler().sendToAll(new UpdateMatchingStatsMessage(conquery.getNamespace().getStorage().getAllConcepts()));
			conquery.waitUntilWorkDone();

			testSpec.executeTest(conquery);
		}

	}

	@Getter
	public static class Sql extends JsonIntegrationTest {

		private final SqlQueryTest testSpec;

		public Sql(InputStream in, TestSqlDialect sqlDialect, SqlConnectorConfig sqlConnectorConfig) throws IOException {
			this.testSpec = TEST_SPEC_READER.readValue(in.readAllBytes());
			this.testSpec.setTableImporter(new CsvTableImporter(sqlDialect.getDSLContext(), sqlDialect, sqlConnectorConfig));
			in.close();
		}

		@Override
		public void execute(StandaloneSupport conquery) throws Exception {
			ConqueryTestSpec testSpec = getTestSpec();
			ValidatorHelper.failOnError(log, VALIDATOR.validate(testSpec));
			testSpec.importRequiredData(conquery);
			testSpec.executeTest(conquery);
		}

		public boolean isAllowedTest(Dialect dialect) {
			SqlQueryTest testSpec = getTestSpec();
			return testSpec.getSupportedDialects() == null || testSpec.getSupportedDialects().contains(dialect);
		}

	}

}
