package com.bakdata.conquery.models.auth.permissions;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * For granting access to the admin servlet.
 *
 */
@Slf4j
@CPSType(id="ADMIN_PERMISSION", base=ConqueryPermission.class)
@ToString(callSuper = true)
public final class AdminPermission extends SpecialPermission {

	public AdminPermission() {
		super();
		log.info("Created AdminPermission");
	}
}
