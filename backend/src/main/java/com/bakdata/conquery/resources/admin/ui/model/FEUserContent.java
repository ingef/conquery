package com.bakdata.conquery.resources.admin.ui.model;

import java.util.List;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class provides the corresponding FreeMarker template with the data needed.
 */
@Getter
@AllArgsConstructor
public class FEUserContent {
	public User self;
	public List<Role> roles;
	public List<ConqueryPermission> permissions;
}