package com.bakdata.conquery.integration;

import java.io.IOException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@CPSBase
public interface ConqueryTestSpec {

	static final Logger log = LoggerFactory.getLogger(ConqueryTestSpec.class);

	public void executeTest(StandaloneSupport support) throws Exception;

	void importRequiredData(StandaloneSupport support) throws Exception;

	@Override
	String toString();

	public default <T> T parseSubTree(StandaloneSupport support, JsonNode node, Class<T> expectedClass) throws IOException, JSONException {
		return parseSubTree(support, node, expectedClass, null);
	}

	public default <T> T parseSubTree(StandaloneSupport support, JsonNode node, Class<T> expectedClass, Consumer<T> modifierBeforeValidation) throws IOException, JSONException {
		return parseSubTree(support, node, Jackson.MAPPER.getTypeFactory().constructParametricType(expectedClass, new JavaType[0]), modifierBeforeValidation);
	}

	public default <T> T parseSubTree(StandaloneSupport support, JsonNode node, JavaType expectedType) throws IOException, JSONException {
		return parseSubTree(support, node, expectedType, null);
	}

	public default <T> T parseSubTree(StandaloneSupport support, JsonNode node, JavaType expectedType, Consumer<T> modifierBeforeValidation) throws IOException, JSONException {
		ObjectMapper mapper = support.getDataset().injectInto(
			new SingletonNamespaceCollection(support.getNamespace().getStorage().getCentralRegistry()).injectInto(
					Jackson.MAPPER.copy()
			)
		);
		T result = mapper.readerFor(expectedType).readValue(node);

		if (modifierBeforeValidation != null) {
			modifierBeforeValidation.accept(result);
		}

		ValidatorHelper.failOnError(log, support.getValidator().validate(result));
		return result;
	}
}
