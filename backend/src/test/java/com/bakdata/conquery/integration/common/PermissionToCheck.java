package com.bakdata.conquery.integration.common;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PermissionToCheck {
	private ConqueryPermission permission;
	private UserId[] permitted;
}
