package com.bakdata.conquery.apiv1.forms;


import static com.bakdata.conquery.apiv1.forms.ExternalForm.Deserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.cps.SubTyped;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormType;
import com.bakdata.conquery.models.forms.managed.ExternalExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@CPSType(id = "EXTERNAL_FORM", base = QueryDescription.class, subTyped = true)
@JsonDeserialize(using = Deserializer.class)
@RequiredArgsConstructor
@Slf4j
public class ExternalForm extends Form implements SubTyped {


	@JsonValue
	private final ObjectNode node;

	private final String subType;

	@Override
	public String getFormType() {
		return CPSTypeIdResolver.createSubTyped(this.getClass().getAnnotation(CPSType.class).id(), getSubType());
	}

	@Override
	public String getLocalizedTypeLabel() {
		final JsonNode formTitle = node.get("title");
		if (formTitle != null && formTitle.isTextual()) {
			return formTitle.asText();
		}

		// Form had no specific title set. Try localized lookup in FormConfig
		Locale preferredLocale = I18n.LOCALE.get();
		FormType frontendConfig = FormScanner.FRONTEND_FORM_CONFIGS.get(this.getFormType());

		if (frontendConfig == null) {
			return getSubType();
		}

		JsonNode titleObj = frontendConfig.getRawConfig().path("title");
		if (!titleObj.isObject()) {
			log.trace("Expected \"title\" member to be of type Object in {}", frontendConfig);
			return getSubType();
		}

		List<Locale> localesFound = new ArrayList<>();
		titleObj.fieldNames().forEachRemaining((lang) -> localesFound.add(new Locale(lang)));
		if (localesFound.isEmpty()) {
			log.trace("Could not extract a locale from the provided FrontendConfig: {}", frontendConfig);
			return getSubType();
		}
		Locale chosenLocale = Locale.lookup(Locale.LanguageRange.parse(preferredLocale.getLanguage()), localesFound);
		if (chosenLocale == null) {
			chosenLocale = localesFound.get(0);
			log.trace("Locale lookup did not return a matching locale. Using the first title encountered: {}", chosenLocale);
		}
		JsonNode title = titleObj.path(chosenLocale.getLanguage());
		if (!title.isTextual()) {
			log.trace("Expected a textual node for the localized title. Was: {}", title.getNodeType());
			return getSubType();
		}
		String ret = title.asText();

		log.trace("Extracted localized title. Was: \"{}\"", ret);
		return ret.isBlank() ? getSubType() : ret;
	}

	@Override
	public ManagedExecution toManagedExecution(User user, Dataset submittedDataset, MetaStorage storage) {
		return new ExternalExecution(this, user, submittedDataset, storage);
	}

	@Override
	public Set<ManagedExecutionId> collectRequiredQueries() {
		return Collections.emptySet();
	}

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public void visit(Consumer<Visitable> visitor) {

	}


	/**
	 * Custom deserializer, that picks up the sub-typing information extracted by
	 * the {@link CPSTypeIdResolver}. This means also that this deserializer
	 * currently can only deserialize secondary types (which is when the property
	 * type of this form is not {@link ExternalForm} but a type higher in the class
	 * hierarchy)
	 */
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Deserializer extends JsonDeserializer<ExternalForm> implements ContextualDeserializer {

		private String subTypeId;

		@Override
		public ExternalForm deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			if (Strings.isNullOrEmpty(subTypeId)) {
				throw new IllegalStateException("This class needs subtype information for deserialization.");
			}
			ObjectNode tree = p.readValueAsTree();
			return new ExternalForm(tree, subTypeId);
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
			// This is only called once per typeId@SubTypeId
			String subTypeId = (String) ctxt.getAttribute(CPSTypeIdResolver.ATTRIBUTE_SUB_TYPE);
			if (Strings.isNullOrEmpty(subTypeId)) {
				throw new IllegalStateException("This class needs subtype information for deserialization.");
			}
			return new Deserializer(subTypeId);
		}

	}
}
