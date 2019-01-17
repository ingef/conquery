package com.bakdata.conquery.resources.admin.ui;

import java.util.List;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FEMandatorContent {

	public List<User> users;
	public List<DatasetPermission> datasetPermissions;
	public List<QueryPermission> queryPermissions;
	public List<ConqueryPermission> otherPermissions;

}