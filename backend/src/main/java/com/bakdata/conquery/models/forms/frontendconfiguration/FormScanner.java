package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.config.ManualConfig;
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
	/**
	 * The config is used to look up the base url for manuals see {@link FrontendConfig#getManualUrl()}.
	 * If the url was changed (e.g. using {@link AdminProcessor#executeScript(String)}) an execution of this
	 * task accounts the change.
	 */
	private final ConqueryConfig config;
	private Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> providerChain = QueryUtils.getNoOpEntryPoint();

	public FormScanner(ConqueryConfig config) {
		super("form-scanner");
		this.config = config;
		registerFrontendFormConfigProvider(ResourceFormConfigProvider::accept);
	}

	public synchronized void registerFrontendFormConfigProvider(Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> provider) {
		providerChain = providerChain.andThen(provider);
	}


	@Nullable
	public static FormType resolveFormType(String formType) {
		return FRONTEND_FORM_CONFIGS.get(formType);
	}

	public static Set<FormType> getAllFormTypes() {
		return Set.copyOf(FRONTEND_FORM_CONFIGS.values());
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		FRONTEND_FORM_CONFIGS = generateFEFormConfigMap();
	}

	private Map<String, FormType> generateFEFormConfigMap() {
		// Collect backend implementations for specific forms
		final Map<String, Class<? extends Form>> forms = findBackendMappingClasses();

		// Collect frontend form configurations for the specific forms
		final List<FormFrontendConfigInformation> frontendConfigs = findFrontendFormConfigs();

		// Match frontend form configurations to backend implementations
		final ImmutableMap.Builder<String, FormType> result = ImmutableMap.builderWithExpectedSize(frontendConfigs.size());


		for (FormFrontendConfigInformation configInfo : frontendConfigs) {

			final ObjectNode configTree = configInfo.getConfigTree();
			final JsonNode type = configTree.get("type");

			if (!validTypeId(type)) {
				log.warn("Found invalid type id in {}. Was: {}", configInfo.getOrigin(), type);
				continue;
			}

			// Extract complete type information (type@subtype) and type information
			final String fullTypeIdentifier = type.asText();
			final String typeIdentifier = CPSTypeIdResolver.truncateSubTypeInformation(fullTypeIdentifier);

			if (!forms.containsKey(typeIdentifier)) {
				log.error("Frontend form config {} (type = {}) does not map to a backend class.", configInfo, type);
				continue;
			}

			// Make relative manual URLs relative to configured handbook base
			// Config url mappings override urls from frontend config jsons
			final URI manualURL = config.getPluginConfig(ManualConfig.class)
										// first check override
										.map(ManualConfig::getForms)
										.map(m -> m.get(fullTypeIdentifier))
										// then query the frontend config json
										.orElseGet(() -> {
											final JsonNode manualUrl = configTree.get(MANUAL_URL_KEY);
											if (manualUrl == null) {
												// Nothing specified, skip
												return null;
											}
											if (!manualUrl.isTextual()) {
												throw new IllegalArgumentException(
														String.format("FrontendFormConfig %s contained field 'manualUrl' but it was not a text. Was: '%s'.", fullTypeIdentifier, manualUrl.getNodeType()));
											}

											return URI.create(manualUrl.textValue());
										});


			final URL manualBaseUrl = config.getFrontend().getManualUrl();

			if (manualBaseUrl != null && manualURL != null) {

				final TextNode manualNode = relativizeManualUrl(fullTypeIdentifier, manualURL, manualBaseUrl);

				if (manualNode == null) {
					log.warn("Manual url relativization did not succeed for {}. Skipping registration.", fullTypeIdentifier);
					continue;
				}

				configTree.set(MANUAL_URL_KEY, manualNode);
			}

			result.put(fullTypeIdentifier, new FormType(fullTypeIdentifier, configTree));

			log.info("Form[{}] from `{}` of Type[{}]", fullTypeIdentifier, configInfo.getOrigin(), forms.get(typeIdentifier).getName());
		}

		return result.build();
	}

	private static Map<String, Class<? extends Form>> findBackendMappingClasses() {
		final Builder<String, Class<? extends Form>> backendClasses = ImmutableMap.builder();
		// Gather form implementations first
		for (Class<?> subclass : CPSTypeIdResolver.SCAN_RESULT.getSubclasses(Form.class.getName()).loadClasses()) {
			if (Modifier.isAbstract(subclass.getModifiers())) {
				continue;
			}

			final CPSType[] cpsAnnotations = subclass.getAnnotationsByType(CPSType.class);

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

	/**
	 * Frontend form configurations can be provided from different sources.
	 * Each source must register a provider with {@link FormScanner#registerFrontendFormConfigProvider(Consumer)} beforehand.
	 */
	@SneakyThrows
	private List<FormFrontendConfigInformation> findFrontendFormConfigs() {

		final ImmutableList.Builder<FormFrontendConfigInformation> frontendConfigs = ImmutableList.builder();

		try {
			providerChain.accept(frontendConfigs);
		}
		catch (Exception e) {
			log.error("Unable to collect all frontend form configurations.", e);
		}
		return frontendConfigs.build();
	}

	private static boolean validTypeId(JsonNode node) {
		return node != null && node.isTextual() && !node.asText().isEmpty();
	}

	private TextNode relativizeManualUrl(@NonNull String formTypeIdentifier, @NonNull URI manualUri, @NonNull URL manualBaseUrl) {

		try {
			if (manualUri.isAbsolute()) {
				log.trace("Manual url for {} was already absolute: {}. Skipping relativization.", formTypeIdentifier, manualUri);
				return new TextNode(manualUri.toURL().toString());
			}

			try {
				final String absoluteUrl = manualBaseUrl.toURI().resolve(manualUri).toURL().toString();
				log.trace("Computed manual url for {}: {}", formTypeIdentifier, absoluteUrl);
				return new TextNode(absoluteUrl);
			}
			catch (URISyntaxException e) {
				log.warn("Unable to resolve manual base url ('{}') and relative manual url ('{}')", manualBaseUrl, manualUri, e);
				return null;
			}
		}
		catch (MalformedURLException e) {
			log.error("Unable to build url", e);
			return null;
		}
	}

}
