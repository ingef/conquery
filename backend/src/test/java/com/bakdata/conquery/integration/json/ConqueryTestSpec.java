package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestSupport;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@Slf4j
@CPSBase
public abstract class ConqueryTestSpec {

	@Nullable
	SqlSpec sqlSpec;
	private String label;
	@Nullable
	private String description;
	@Nullable
	private ConqueryConfig config;
	// default IdColumnConfig for SQL mode
	private IdColumnConfig idColumns = null;

	public static <T> T parseSubTree(TestSupport support, JsonNode node, Class<T> expectedClass, boolean injectResolvers)
			throws IOException {
		return parseSubTree(support, node, expectedClass, null, injectResolvers);
	}

	public static <T> T parseSubTree(
			TestSupport support,
			JsonNode node,
			Class<T> expectedClass,
			Consumer<T> modifierBeforeValidation,
			boolean injectResolvers
	) throws IOException {
		return parseSubTree(support, node, Jackson.MAPPER.getTypeFactory()
														 .constructParametricType(expectedClass, new JavaType[0]), modifierBeforeValidation, injectResolvers);
	}

	public static <T> T parseSubTree(TestSupport support, JsonNode node, JavaType expectedType, Consumer<T> modifierBeforeValidation,
									 boolean injectResolvers) throws IOException {
		final ObjectMapper om = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		final ObjectMapper mapper = om.addHandler(new DatasetPlaceHolderFiller(support));

		support.getConfig().injectInto(mapper);
		support.getNamespace().getDataset().injectInto(mapper);

		if (injectResolvers) {
			support.getMetaStorage().injectInto(mapper);
			support.getNamespace().getStorage().injectInto(mapper);
			support.getDatasetRegistry().injectInto(mapper);
		}

		T result = mapper.readerFor(expectedType).readValue(node);

		if (modifierBeforeValidation != null) {
			modifierBeforeValidation.accept(result);
		}

		if (injectResolvers) {
			// With placeholders the validation likely fails, so we skip it there
			ValidatorHelper.failOnError(log, support.getValidator().validate(result));
		}
		return result;
	}

	public static <T> T parseSubTree(TestSupport support, JsonNode node, JavaType expectedType, boolean injectResolvers)
			throws IOException {
		return parseSubTree(support, node, expectedType, null, injectResolvers);
	}

	public static <T> List<T> parseSubTreeList(TestSupport support, ArrayNode node, Class<?> expectedType, Consumer<T> modifierBeforeValidation)
			throws IOException {
		final ObjectMapper om = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		final ObjectMapper mapper = om.addHandler(new DatasetPlaceHolderFiller(support));

		// Inject dataset, so that namespaced ids that are not prefixed with in the test-spec are get prefixed
		support.getNamespace().injectInto(mapper);

		mapper.setConfig(mapper.getDeserializationConfig().withView(View.Api.class));

		List<T> result = new ArrayList<>(node.size());
		for (var child : node) {
			T value;
			try {
				value = mapper.readerFor(expectedType).readValue(child);
			}
			catch (Exception e) {
				if (child.isValueNode()) {
					String potentialPath = child.textValue();
					try {
						value = mapper.readerFor(expectedType).readValue(IntegrationTest.class.getResource(potentialPath));
					}
					catch (Exception e2) {
						throw new RuntimeException("Could not parse value " + potentialPath, e2);
					}
				}
				else {
					throw e;
				}
			}

			if (modifierBeforeValidation != null) {
				modifierBeforeValidation.accept(value);
			}
			result.add(value);
		}
		return result;
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
		return label;
	}

	public boolean isEnabled(Dialect sqlDialect) {
		return sqlSpec == null || sqlSpec.isEnabled() && sqlSpec.isAllowedTest(sqlDialect);
	}

	/**
	 * Replaces occurrences of the string "${dataset}" with the id of the current dataset of the {@link StandaloneSupport}.
	 */
	@RequiredArgsConstructor
	private static class DatasetPlaceHolderFiller extends DeserializationProblemHandler {

		private final TestSupport support;

		@Override
		public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) {
			IdUtil.Parser<?> parser = IdUtil.<Id<Identifiable<?, ?>, ?>>createParser((Class) targetType);
			return parser.parsePrefixed(support.getDataset().getName(), valueToConvert);
		}
	}
}
