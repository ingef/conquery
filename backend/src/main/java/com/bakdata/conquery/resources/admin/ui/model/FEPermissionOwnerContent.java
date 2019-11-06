package com.bakdata.conquery.resources.admin.ui.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * This class provides the FreeMarker template with the needed data for easier access.
 */
@Getter
@SuperBuilder
public class FEPermissionOwnerContent<OWNER extends PermissionOwner<?>> {
	private OWNER owner;

	/**
	 *  Holds the owned permission objects and its JSON representation.
	 */
	private List<Pair<FEPermission,String>> permissions;
	
	
	/**
	 *  Holds possible permission types, their abilities and targets that can be used for the creation of a permission.
	 */
	private Map<String, Pair<Set<Ability>,List<Object>>> permissionTemplateMap;
	
	
}
