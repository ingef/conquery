package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Set;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * This class provides the corresponding FreeMarker template with the data needed.
 */
@Getter
@SuperBuilder
public class FEGroupContent extends FEPermissionOwnerContent<Group> {

	public Set<User> member;
}