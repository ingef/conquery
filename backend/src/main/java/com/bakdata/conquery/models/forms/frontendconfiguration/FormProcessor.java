package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigProvider.FormFrontendConfigInformation;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Getter
@Slf4j
public class FormProcessor {

	private final static String INFO_FORMAT = "\t%-20s %-60s %-20s\n";
	private final static ObjectReader READER = io.dropwizard.jackson.Jackson.newObjectMapper().reader();
	private static final Map<String, JsonNode> FRONTEND_FORM_CONFIGS = generateFEFormConfigMap();

	private final MasterMetaStorage storage;

	private static Map<String, Class<? extends Form>> findBackendMappingClasses() {
		// Gather form implementations first
		Map<String, Class<? extends Form>> forms = new HashMap<>();
		for (Class<?> subclass : CPSTypeIdResolver.SCAN_RESULT.getClassesImplementing(Form.class.getName()).loadClasses()) {
			if (Modifier.isAbstract(subclass.getModifiers())) {
				continue;
			}
			CPSType anno = subclass.getAnnotation(CPSType.class);
			if (anno == null) {
				log.warn("Implemented Form {} has no CPSType annotation", subclass);
				continue;
			}
			forms.put(anno.id(), (Class<? extends Form>) subclass);
		}
		return forms;
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
		
		Consumer<Collection<FormFrontendConfigInformation>> providerChain = QueryUtils.getNoOpEntryPoint();
		
		for (Class<?> configProvider : configProviders) {
			int modifiers = configProvider.getModifiers();
			if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Class<? extends FormFrontendConfigProvider> formConfigProvider = (Class<? extends FormFrontendConfigProviderBase>) configProvider;
			Consumer<Collection<FormFrontendConfigInformation>> provider;
			// Distinguish between interface implementations and implementations of the abstract class 
			if (FormFrontendConfigProviderBase.class.isAssignableFrom(formConfigProvider)) {
				provider = formConfigProvider.getConstructor(ObjectReader.class).newInstance(READER);
			}
			else {
				provider = formConfigProvider.getConstructor().newInstance();
			}
			providerChain = providerChain.andThen(provider);
		}
		List<FormFrontendConfigInformation> frontendConfigs = new ArrayList<>();
		providerChain.accept(frontendConfigs);
		return frontendConfigs;
	}

	private static Map<String, JsonNode> generateFEFormConfigMap() {
		StringBuilder info = new StringBuilder(String.format("\n" + INFO_FORMAT, "Form Type", "Frontend Config", "Backend Class"));

		// Collect backend implementations for specific forms
		Map<String, Class<? extends Form>> forms = findBackendMappingClasses();

		// Collect frontend form configurations for the specific forms
		List<FormFrontendConfigInformation> frontendConfigs = findFrontendFormConfigs();

		// Match frontend form configurations to backend implementations
		Map<String, JsonNode> result = new HashMap<>();
		for (FormFrontendConfigInformation configInfo : frontendConfigs) {
			JsonNode configTree = configInfo.getConfigTree();
			String typeIdentifier = null;
			JsonNode type = configTree.get("type");
			if (validTypeId(type)) {
				typeIdentifier = type.asText();
			}
			if (!forms.containsKey(typeIdentifier)) {
				log.warn("Frontend form config {} (type = {}) does not map to a backend class.", configInfo, type);
				continue;
			}
			JsonNode subtype = configTree.get("subType");
			if (validTypeId(subtype)) {
				typeIdentifier = subtype.asText();
			}
			JsonNode prev = result.put(typeIdentifier, configTree);
			if (prev != null) {
				throw new IllegalStateException(String.format(
					"Could not map %s to form %s because there was already a mapping:\n%s",
					configInfo.getOrigin(),
					typeIdentifier,
					prev));
			}
			Class<? extends Form> formClass = forms.get(type.asText());
			info.append(String.format(INFO_FORMAT, typeIdentifier, configInfo.getOrigin(), formClass.getName()));
		}
		log.info(info.toString());
		return result;
	}

	private static boolean validTypeId(JsonNode node) {
		return node != null && node.isTextual() && !node.asText().isEmpty();
	}

	public Collection<JsonNode> getFormsForUser(User user) {
		List<JsonNode> allowedForms = new ArrayList<>();

		for (Entry<String, JsonNode> formMapping : FRONTEND_FORM_CONFIGS.entrySet()) {
			if (user.isPermitted(FormPermission.onInstance(Ability.CREATE, formMapping.getKey()))) {
				allowedForms.add(formMapping.getValue());
			}
		}

		return allowedForms;

	}

}
