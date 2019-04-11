package com.bakdata.conquery.integration.json;

import java.io.IOException;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

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
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Slf4j @CPSBase
public abstract class ConqueryTestSpec {
	
	@Getter @Setter @NotNull
	private String label;

	public abstract void executeTest(StandaloneSupport support) throws Exception;

	public abstract void importRequiredData(StandaloneSupport support) throws Exception;

	@Override
	public String toString() {
		return label;
	}

	public <T> T parseSubTree(StandaloneSupport support, JsonNode node, Class<T> expectedClass) throws IOException, JSONException {
		return parseSubTree(support, node, expectedClass, null);
	}

	public <T> T parseSubTree(StandaloneSupport support, JsonNode node, Class<T> expectedClass, Consumer<T> modifierBeforeValidation) throws IOException, JSONException {
		return parseSubTree(support, node, Jackson.MAPPER.getTypeFactory().constructParametricType(expectedClass, new JavaType[0]), modifierBeforeValidation);
	}

	public <T> T parseSubTree(StandaloneSupport support, JsonNode node, JavaType expectedType) throws IOException, JSONException {
		return parseSubTree(support, node, expectedType, null);
	}

	public <T> T parseSubTree(StandaloneSupport support, JsonNode node, JavaType expectedType, Consumer<T> modifierBeforeValidation) throws IOException, JSONException {
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
