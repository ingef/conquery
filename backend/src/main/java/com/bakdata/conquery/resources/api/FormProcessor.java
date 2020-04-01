package com.bakdata.conquery.resources.api;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Getter @Slf4j
public class FormProcessor {

	private final Namespaces namespaces;
	private final MasterMetaStorage storage;
	private static final Map<Class<? extends Form>, JsonNode> FRONTEND_FORM_CONFIGS = generateFEFormConfigMap();

	private static Map<Class<? extends Form>, JsonNode> generateFEFormConfigMap() {
		Map<String, Class<? extends Form>> forms = new HashMap<>();
		for( Class<?> subclass : CPSTypeIdResolver.SCAN_RESULT.getSubclasses(Form.class.getName()).loadClasses()) {
			if(Modifier.isAbstract(subclass.getModifiers())){
				continue;
			}
			CPSType anno = subclass.getAnnotation(CPSType.class);
			if(anno == null) {
				log.warn("Implemented Form {} has no CPSType annotation", subclass);
				continue;
			}
			forms.put(anno.id(), (Class<? extends Form>) subclass);
		}
		
		Map<Class<? extends Form>, JsonNode> result = new HashMap<>();
		ResourceList frontendConfigs = CPSTypeIdResolver.SCAN_RESULT
			.getResourcesMatchingPattern(Pattern.compile(".*\\.frontend_conf\\.json"));
		ObjectReader reader = io.dropwizard.jackson.Jackson.newObjectMapper().reader();
		final String infoFormat = "\t%-20s %-50s %-20s\n";
		StringBuilder info = new StringBuilder(String.format(infoFormat, "Form Type", "Frontend Config", "Backend Class"));
		for (Resource config : frontendConfigs) {
			JsonNode configTree;
			try {
				configTree = reader.readTree(config.open());
			}
			catch (IOException e) {
				throw new IllegalArgumentException(String.format("Could not parse the frontend config: %s", config.getPath()), e);
			}
			String formType = configTree.get("type").asText();
			Class<? extends Form> formClass = forms.get(formType);
			if(formClass == null) {
				throw new IllegalStateException(String.format("Found frontend config for form %s but could not find an corresponding backend implementation.", formType));
			}
			JsonNode prev = result.put(formClass, configTree);
			if(prev != null ) {				
				throw new IllegalStateException(String.format("Could not map %s to form %s because there was already a mapping:\n%s", config.getPathRelativeToClasspathElement(), formType, prev));
			}
			info.append(String.format(infoFormat, formType, config.getPath(), formClass));
		}
		log.info("Found form config mapping for form:\n{}", info.toString());
		
		return result;
	}

	public Collection<JsonNode> getFormsForUser(User user) {
		List<JsonNode> allowedForms = new ArrayList<>();
		
		for(Entry<Class<? extends Form>, JsonNode> formMapping : FRONTEND_FORM_CONFIGS.entrySet()) {			
			if (user.isPermitted(FormPermission.onInstance(Ability.CREATE, formMapping.getKey()))) {
				allowedForms.add(formMapping.getValue());
			}
		}

		return allowedForms;

	}
}
