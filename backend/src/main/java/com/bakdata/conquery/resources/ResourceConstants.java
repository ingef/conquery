package com.bakdata.conquery.resources;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class ResourceConstants {

	public static final String DATASET	=	"dataset";
	public static final String CONCEPT	=	"concept";
	public static final String TABLE	=	"table";
	public static final String FILTER	=	"filter";
	public static final String QUERY	=	"query";
	public static final String FORM_CONFIG	=	"form-config";
	public static final String FILENAME	=	"filename";
	public static final String API		=	"api";
	public static final String IMPORT_ID = "importId";
	public static final String JOB_ID = "jobId";
	public static final String OWNER_ID = "ownerId";
	public static final String GROUPS_PATH_ELEMENT = "groups";
	public static final String USERS_PATH_ELEMENT = "users";
	public static final String ROLES_PATH_ELEMENT = "roles";
	public static final String USER_PATH_ELEMENT = "user";
	public static final String ROLE_PATH_ELEMENT = "role";
	public static final String AUTH_OVERVIEW_PATH_ELEMENT = "auth-overview";
	public static final String USER_ID = "userId";
	public static final String ROLE_ID = "roleId";
	public static final String GROUP_ID = "groupId";

	/**
	 * Method to generate a data-model of this class's static members so that they
	 * are accessible from within a freemarker template.
	 */
	public static final TemplateModel getAsTemplateModel() {
		try {
			return new DefaultObjectWrapperBuilder(Configuration.getVersion())
				.build()
				.getStaticModels()
				.get(ResourceConstants.class.getName());
		}
		catch (TemplateModelException e) {
			throw new IllegalStateException("Could not generate template model for " + ResourceConstants.class.getName(), e);
		}
	}
}
