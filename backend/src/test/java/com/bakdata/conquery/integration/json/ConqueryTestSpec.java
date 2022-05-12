package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.StandaloneSupport;
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

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Slf4j
@CPSBase
public abstract class ConqueryTestSpec {

	@Getter
	@Setter
	@NotNull
	private String label;

	@Setter
	@Getter
	@Nullable
	private ConqueryConfig config;

	public ConqueryConfig overrideConfig(ConqueryConfig config) {

		if (getConfig() != null) {
			final ConqueryConfig conqueryConfig = getConfig().withStorage(new NonPersistentStoreFactory());
			conqueryConfig.setLoggingFactory(config.getLoggingFactory());
			return conqueryConfig;
		}

		return config.withStorage(new NonPersistentStoreFactory());
	}

	public abstract void executeTest(StandaloneSupport support) throws Exception;

	public abstract void importRequiredData(StandaloneSupport support) throws Exception;


	@Override
	public String toString() {
		return label;
	}

	public static <T> T parseSubTree(StandaloneSupport support, JsonNode node, Class<T> expectedClass) throws IOException, JSONException {
		return parseSubTree(support, node, expectedClass, null);
	}

	public static <T> T parseSubTree(StandaloneSupport support, JsonNode node, Class<T> expectedClass, Consumer<T> modifierBeforeValidation)
			throws IOException, JSONException {
		return parseSubTree(support, node, support.getObjectMapper()
												  .getTypeFactory().constructParametricType(expectedClass, new JavaType[0]), modifierBeforeValidation);
	}

	public static <T> T parseSubTree(StandaloneSupport support, JsonNode node, JavaType expectedType) throws IOException, JSONException {
		return parseSubTree(support, node, expectedType, null);
	}

	public static <T> T parseSubTree(StandaloneSupport support, JsonNode node, JavaType expectedType, Consumer<T> modifierBeforeValidation)
			throws IOException, JSONException {
		ObjectMapper mapper = support.getDataset().injectIntoNew(
				new SingletonNamespaceCollection(support.getNamespace().getStorage().getCentralRegistry(), support.getMetaStorage().getCentralRegistry())
						.injectIntoNew(
								support.getObjectMapper().addHandler(new DatasetPlaceHolderFiller(support))
						)
		);

		T result = mapper.readerFor(expectedType).readValue(node);

		if (modifierBeforeValidation != null) {
			modifierBeforeValidation.accept(result);
		}

		ValidatorHelper.failOnError(log, support.getValidator().validate(result));
		return result;
	}

	public static <T> List<T> parseSubTreeList(StandaloneSupport support, ArrayNode node, Class<?> expectedType, Consumer<T> modifierBeforeValidation)
			throws IOException, JSONException {

		ObjectMapper mapper = support.getDataset()
									 .injectIntoNew(
											 new SingletonNamespaceCollection(support.getNamespace().getStorage().getCentralRegistry())
													 .injectIntoNew(
															 support.getObjectMapper()
																	.addHandler(new DatasetPlaceHolderFiller(support))
													 )
									 );
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
			ValidatorHelper.failOnError(log, support.getValidator().validate(value));
		}
		return result;
	}


	/**
	 * Replaces occurrences of the string "${dataset}" with the id of the current dataset of the {@link StandaloneSupport}.
	 */
	@RequiredArgsConstructor
	private static class DatasetPlaceHolderFiller extends DeserializationProblemHandler {

		private final StandaloneSupport support;

		@Override
		public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) throws IOException {
			IId.Parser parser = IId.<IId<Identifiable<?>>>createParser((Class) targetType);
			return parser.parsePrefixed(support.getDataset().getId().toString(), valueToConvert);
		}
	}
}
