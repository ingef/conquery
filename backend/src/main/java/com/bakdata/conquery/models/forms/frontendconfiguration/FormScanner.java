package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import io.dropwizard.servlets.tasks.Task;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FormScanner extends Task {

	public static final String MANUAL_URL_KEY = "manualUrl";
	public static Map<String, FormType> FRONTEND_FORM_CONFIGS = Collections.emptyMap();

	private Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> providerChain = QueryUtils.getNoOpEntryPoint();

	/**
	 * The config is used to look up the base url for manuals see {@link FrontendConfig#getManualUrl()}.
	 * If the url was changed (e.g. using {@link AdminProcessor#executeScript(String)}) an execution of this
	 * task accounts the change.
	 */
	private final ConqueryConfig config;

	public FormScanner(ConqueryConfig config) {
		super("form-scanner");
		this.config = config;
		registerFrontendFormConfigProvider(ResourceFormConfigProvider::accept);
	}

	private static Map<String, Class<? extends Form>> findBackendMappingClasses() {
		Builder<String, Class<? extends Form>> backendClasses = ImmutableMap.builder();
		// Gather form implementations first
		for (Class<?> subclass : CPSTypeIdResolver.SCAN_RESULT.getSubclasses(Form.class.getName()).loadClasses()) {
			if (Modifier.isAbstract(subclass.getModifiers())) {
				continue;
			}
			CPSType[] cpsAnnotations = subclass.getAnnotationsByType(CPSType.class);

			if (cpsAnnotations.length == 0) {
				log.warn("Implemented Form {} has no CPSType annotation", subclass);
				continue;
			}
			for (CPSType cpsType : cpsAnnotations) {
				backendClasses.put(cpsType.id(), (Class<? extends Form>) subclass);
			}
		}
		return backendClasses.build();
	}

	public synchronized void registerFrontendFormConfigProvider(Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> provider){
		providerChain = providerChain.andThen(provider);
	}

	/**
	 * Frontend form configurations can be provided from different sources.
	 * Each source must register a provider with {@link FormScanner#registerFrontendFormConfigProvider(Consumer)} beforehand.
	 */
	@SneakyThrows
	private List<FormFrontendConfigInformation> findFrontendFormConfigs() {

		ImmutableList.Builder<FormFrontendConfigInformation> frontendConfigs = ImmutableList.builder();
		try {
			providerChain.accept(frontendConfigs);
		} catch (Exception e) {
			log.error("Unable to collect all frontend form configurations.", e);
		}
		return frontendConfigs.build();
	}

	private Map<String, FormType> generateFEFormConfigMap() {
		// Collect backend implementations for specific forms
		Map<String, Class<? extends Form>> forms = findBackendMappingClasses();

		// Collect frontend form configurations for the specific forms
		List<FormFrontendConfigInformation> frontendConfigs = findFrontendFormConfigs();

		// Match frontend form configurations to backend implementations
		final ImmutableMap.Builder<String, FormType> result = ImmutableMap.builderWithExpectedSize(frontendConfigs.size());


		for (FormFrontendConfigInformation configInfo : frontendConfigs) {
			ObjectNode configTree = configInfo.getConfigTree();
			JsonNode type = configTree.get("type");
			if (!validTypeId(type)) {
				log.warn("Found invalid type id in {}. Was: {}", configInfo.getOrigin(), type);
				continue;
			}

			// Extract complete type information (type@subtype) and type information
			String fullTypeIdentifier = type.asText();
			String typeIdentifier = CPSTypeIdResolver.truncateSubTypeInformation(fullTypeIdentifier);
			if (!forms.containsKey(typeIdentifier)) {
				log.error("Frontend form config {} (type = {}) does not map to a backend class.", configInfo, type);
				continue;
			}

			// Make relative handbook URLs relative to configured handbook base
			final JsonNode manualUrl = configTree.get(MANUAL_URL_KEY);
			final URL manualBaseUrl = config.getFrontend().getManualUrl();
			if (manualBaseUrl != null && manualUrl != null) {
				final TextNode manualNode = relativizeManualUrl(fullTypeIdentifier, manualUrl, manualBaseUrl);
				if (manualNode == null) {
					log.warn("Manual url relativiation did not succeed for {}. Skipping registration.", fullTypeIdentifier);
					continue;
				}
				configTree.set(MANUAL_URL_KEY, manualNode);
			}

			result.put(fullTypeIdentifier, new FormType(fullTypeIdentifier, configTree));

			log.info("Form[{}] from `{}` of Type[{}]", fullTypeIdentifier, configInfo.getOrigin(), forms.get(typeIdentifier).getName());
		}

		return result.build();
	}

	private TextNode relativizeManualUrl(@NonNull String formTypeIdentifier, @NonNull JsonNode manualUrl, @NonNull URL manualBaseUrl) {
		if (!manualUrl.isTextual()) {
			log.warn("FrontendFormConfig {} contained field 'manualUrl' but it was not a text. Was: '{}'.", formTypeIdentifier, manualUrl.getNodeType());
			return null;
		}

		final String urlString = manualUrl.textValue();
		final URI manualUri = URI.create(urlString);
		if (manualUri.isAbsolute()) {
			log.trace("Manual url for {} was already absolute: {}. Skipping relativization.", formTypeIdentifier, manualUri);
			return new TextNode(urlString);
		}

		try {
			final String absoluteUrl = manualBaseUrl.toURI().resolve(manualUri).toString();
			log.trace("Computed manual url for {}: {}", formTypeIdentifier, absoluteUrl);
			return new TextNode(absoluteUrl);
		}
		catch (URISyntaxException e) {
			log.warn("Unable to resolve manual base url ('{}') and relative manual url ('{}')", manualBaseUrl, manualUri, e);
			return null;
		}
	}

	private static boolean validTypeId(JsonNode node) {
		return node != null && node.isTextual() && !node.asText().isEmpty();
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		FRONTEND_FORM_CONFIGS = generateFEFormConfigMap();
	}

}
