package com.bakdata.conquery.resources.admin.ui;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class provides the corresponding FreeMarker template with the data needed.
 */
@Getter
@AllArgsConstructor
public class FEMandatorContent {

	public Mandator self;
	public List<User> users;
	public List<DatasetPermission> datasetPermissions;
	public List<QueryPermission> queryPermissions;
	public List<ConqueryPermission> otherPermissions;
	
	public Set<Ability> abilities;
	public List<Dataset> datasets;

}