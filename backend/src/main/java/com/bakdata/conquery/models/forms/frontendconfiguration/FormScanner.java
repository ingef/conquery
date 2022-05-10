package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import io.dropwizard.servlets.tasks.Task;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FormScanner extends Task {
	
	public static Map<String, FormType> FRONTEND_FORM_CONFIGS = Collections.emptyMap();


	private Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> providerChain = QueryUtils.getNoOpEntryPoint();


	public FormScanner(ObjectReader reader) {
		super("form-scanner");
		registerFrontendFormConfigProvider(formConfigInfos -> ResourceFormConfigProvider.accept(formConfigInfos, reader));
	}

	private static Map<String, Class<? extends Form>> findBackendMappingClasses() {
		Builder<String, Class<? extends Form>> backendClasses = ImmutableMap.builder();
		// Gather form implementations first
		for (Class<?> subclass : CPSTypeIdResolver.SCAN_RESULT.getSubclasses(Form.class.getName()).loadClasses()) {
			if (Modifier.isAbstract(subclass.getModifiers())) {
				continue;
			}
			CPSType anno = subclass.getAnnotation(CPSType.class);
			if (anno == null) {
				log.warn("Implemented Form {} has no CPSType annotation", subclass);
				continue;
			}
			backendClasses.put(anno.id(), (Class<? extends Form>) subclass);
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
			JsonNode configTree = configInfo.getConfigTree();
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

			result.put(fullTypeIdentifier, new FormType(fullTypeIdentifier, configTree));
			
			log.info("Form[{}] from `{}` of Type[{}]", fullTypeIdentifier, configInfo.getOrigin(), forms.get(typeIdentifier).getName());
		}

		return result.build();
	}

	private static boolean validTypeId(JsonNode node) {
		return node != null && node.isTextual() && !node.asText().isEmpty();
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		FRONTEND_FORM_CONFIGS = generateFEFormConfigMap();
	}

}
