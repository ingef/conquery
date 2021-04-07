package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigProvider.FormFrontendConfigInformation;
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

	public FormScanner() {
		super("form-scanner");
	}

	private final static String INFO_FORMAT = "\t%-30s %-60s %-20s";
	private final static ObjectReader READER = Jackson.MAPPER.copy().reader();
	
	public static Map<String, JsonNode> FRONTEND_FORM_CONFIGS = ImmutableMap.of();

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

	/**
	 * Frontend form configurations can be provided from different sources. For
	 * these sources a stub of type {@link FormFrontendConfigProviderBase} must be
	 * implemented in order to be discovered by this function.
	 */
	@SneakyThrows
	private static List<FormFrontendConfigInformation> findFrontendFormConfigs() {
		List<Class<?>> configProviders = CPSTypeIdResolver.SCAN_RESULT.getClassesImplementing(FormFrontendConfigProvider.class.getName())
			.loadClasses();
		
		Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> providerChain = QueryUtils.getNoOpEntryPoint();
		
		for (Class<?> configProvider : configProviders) {
			int modifiers = configProvider.getModifiers();
			if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Class<? extends FormFrontendConfigProvider> formConfigProvider = (Class<? extends FormFrontendConfigProviderBase>) configProvider;
			Consumer<ImmutableCollection.Builder<FormFrontendConfigInformation>> provider;
			// Distinguish between interface implementations and implementations of the abstract class 
			if (FormFrontendConfigProviderBase.class.isAssignableFrom(formConfigProvider)) {
				provider = formConfigProvider.getConstructor(ObjectReader.class).newInstance(READER);
			}
			else {
				provider = formConfigProvider.getConstructor().newInstance();
			}
			providerChain = providerChain.andThen(provider);
		}

		ImmutableList.Builder<FormFrontendConfigInformation> frontendConfigs = ImmutableList.builder();
		try {
			providerChain.accept(frontendConfigs);			
		} catch (Exception e) {
			log.error("Unable to collect all frontend form configurations.", e);
		}
		return frontendConfigs.build();
	}

	private static Map<String, JsonNode> generateFEFormConfigMap() {
		StringJoiner info = new StringJoiner("\n", "\n", "\n");
		info.add(String.format(INFO_FORMAT, "Form Type", "Frontend Config", "Backend Class"));
		

		// Collect backend implementations for specific forms
		Map<String, Class<? extends Form>> forms = findBackendMappingClasses();

		// Collect frontend form configurations for the specific forms
		List<FormFrontendConfigInformation> frontendConfigs = findFrontendFormConfigs();

		// Match frontend form configurations to backend implementations
		ImmutableMap.Builder<String, JsonNode> result = ImmutableMap.builder();
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
				log.warn("Frontend form config {} (type = {}) does not map to a backend class.", configInfo, type);
				continue;
			}
			
			// Register Fontend config and check if there was already a mapping for this complete type to a frontend config
			result.put(fullTypeIdentifier, configTree);
			
			// Update information string
			info.add(String.format(INFO_FORMAT, fullTypeIdentifier, configInfo.getOrigin(), forms.get(typeIdentifier).getName()));
		}
		log.info(info.toString());
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
