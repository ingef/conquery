package com.bakdata.conquery.integration.json;

import java.io.IOException;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectReader;
import io.github.classgraph.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.IoUtil;

@Setter
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@Slf4j
@CPSBase
public abstract class ConqueryTestSpec {

	private static final ObjectReader TEST_SPEC_READER = Jackson.MAPPER.readerFor(ConqueryTestSpec.class);


	@Nullable
	SqlSpec sqlSpec;
	private String label;
	@JsonIgnore
	private String source;
	@Nullable
	private String description;
	@Nullable
	private ConqueryConfig config;
	// default IdColumnConfig for SQL mode
	private IdColumnConfig idColumns = null;

	public static ConqueryTestSpec fromResourcePath(String resource) {
		try {
			ConqueryTestSpec spec = TEST_SPEC_READER.readValue(IOUtils.resourceToURL(resource).openStream());
			spec.setSource(resource);
			return spec;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static ConqueryTestSpec fromResource(Resource resource) {
		try {
			ConqueryTestSpec spec = TEST_SPEC_READER.readValue(resource.open());
			spec.setSource(resource.getURI().toString());
			return spec;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T extends ConqueryTestSpec> T readJson(DatasetId dataset, String json) throws IOException {
		return readJson(dataset, json, TEST_SPEC_READER);
	}

	private static <T extends ConqueryTestSpec> T readJson(DatasetId dataset, String json, ObjectReader jsonReader) throws IOException {
		json = StringUtils.replace(
				json,
				"${dataset}",
				dataset.toString()
		);

		T spec = jsonReader.readValue(json);
		spec.setSource(json);
		return spec;
	}

	public static <T extends ConqueryTestSpec> T readJson(Dataset dataset, String json) throws IOException {
		return readJson(dataset.getId(), json, dataset.injectIntoNew(TEST_SPEC_READER));
	}

	public ConqueryConfig overrideConfig(ConqueryConfig config) {

		if (getConfig() != null) {
			final ConqueryConfig conqueryConfig = getConfig().withStorage(new NonPersistentStoreFactory());
			conqueryConfig.setLoggingFactory(config.getLoggingFactory());
			return conqueryConfig;
		}

		final IdColumnConfig idColumnConfig = idColumns != null ? idColumns : config.getIdColumns();
		return config.withIdColumns(idColumnConfig)
					 .withStorage(new NonPersistentStoreFactory());
	}

	public abstract void executeTest(StandaloneSupport support) throws Exception;

	public abstract void importRequiredData(StandaloneSupport support) throws Exception;

	@Override
	public String toString() {
		return String.format("%s#%s", source, label);
	}

	public boolean isEnabled(Dialect sqlDialect) {
		return sqlSpec == null || sqlSpec.isEnabled() && sqlSpec.isAllowedTest(sqlDialect);
	}

}
