package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.fasterxml.jackson.databind.JsonNode;

public class FormProcessor {

	public Collection<JsonNode> getFormsForUser(Subject subject) {
		List<JsonNode> allowedForms = new ArrayList<>();

		for (FormType formMapping : FormScanner.getAllFormTypes()) {
			if (!subject.isPermitted(formMapping, Ability.CREATE)) {
				continue;
			}

			allowedForms.add(formMapping.getRawConfig());
		}

		return allowedForms;

	}

}
