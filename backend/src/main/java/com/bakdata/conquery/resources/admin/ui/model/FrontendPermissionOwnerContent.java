package com.bakdata.conquery.resources.admin.ui.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.permissions.Ability;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This class provides the FreeMarker template with the needed data for easier access.
 */
@Getter
@SuperBuilder
public class FrontendPermissionOwnerContent<OWNER extends PermissionOwner<?>> {
	private OWNER owner;

	/**
	 * Holds the owned permission objects and its JSON representation.
	 */
	private Set<FrontendPermission> permissions;


	/**
	 *  Holds possible permission types, their abilities and targets that can be used for the creation of a permission.
	 */
	private Map<String, Pair<Set<Ability>,List<Object>>> permissionTemplateMap;
	
	
}
