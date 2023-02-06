package com.bakdata.conquery.models.forms.frontendconfiguration;

import static com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner.FRONTEND_FORM_CONFIGS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.fasterxml.jackson.databind.JsonNode;

public class FormProcessor {

	public Collection<JsonNode> getFormsForUser(Subject subject) {
		List<JsonNode> allowedForms = new ArrayList<>();

		for (FormType formMapping : FRONTEND_FORM_CONFIGS.values()) {
			if (!subject.isPermitted(formMapping, Ability.CREATE)) {
				continue;
			}

			allowedForms.add(formMapping.getRawConfig());
		}

		return allowedForms;

	}

}
