package com.bakdata.conquery.resources.admin.ui.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class provides the corresponding FreeMarker template with the data needed.
 */
@Getter
@AllArgsConstructor
public class FERoleContent {
	public Role self;
	public List<User> users;
	public List<DatasetPermission> datasetPermissions;
	public List<QueryPermission> queryPermissions;
	public List<ConqueryPermission> otherPermissions;
	
	public Set<Ability> abilities;
	
	Map<String, Pair<Set<Ability>,List<Object>>> permissionTemplateMap;
}