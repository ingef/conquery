package com.bakdata.conquery.models.forms.frontendconfiguration;

import static com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner.FRONTEND_FORM_CONFIGS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.fasterxml.jackson.databind.JsonNode;

public class FormProcessor {

	public Collection<JsonNode> getFormsForUser(User user) {
		List<JsonNode> allowedForms = new ArrayList<>();

		for (Entry<String, JsonNode> formMapping : FRONTEND_FORM_CONFIGS.entrySet()) {
			if (AuthorizationHelper.isPermitted(user, FormPermission.onInstance(Ability.CREATE, formMapping.getKey()))) {
				allowedForms.add(formMapping.getValue());
			}
		}

		return allowedForms;

	}

}
