package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.util.Set;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NonNull;

@Data
public class FormType implements Authorized {
	@NonNull
	private final String name;
	private final JsonNode rawConfig;

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return FormPermission.onInstance(abilities, name);
	}
}
