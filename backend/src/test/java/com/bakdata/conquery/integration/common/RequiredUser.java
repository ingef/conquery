package com.bakdata.conquery.integration.common;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RequiredUser {
	/**
	 * The actual user is wrapped and the supplied role is injected after the user is parsed.
	 * This eases the test writing, since no accidental valid roles are constructed, which are
	 * not previously parsed.
	 */
	@Valid @NotNull
	private User user;
	@Valid
	private RoleId [] rolesInjected = new RoleId[0];
}
