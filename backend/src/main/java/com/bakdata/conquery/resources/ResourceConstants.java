package com.bakdata.conquery.resources;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class ResourceConstants {
	public static final String DATASET_NAME = "datasetName";
	public static final String TABLE_NAME = "tableName";
	public static final String CONCEPT_NAME = "conceptName";
	public static final String IMPORT_ID = "importId";
	public static final String FILTER_NAME = "filterName";
	public static final String JOB_ID = "jobId";
	public static final String OWNER_ID = "ownerId";
	public static final String USER_PATH_ELEMENT = "user";
	public static final String ROLE_PATH_ELEMENT = "role";
	public static final String USER_ID = "userId";
	public static final String ROLE_ID = "roleId";
	public static final String GROUP_ID = "groupId";

	/**
	 * Method to generate a data-model of this class's static members so that they
	 * are accessible from within a freemarker template.
	 * 
	 * @return
	 */
	public final static TemplateModel getAsTemplateModel() {
		try {
			return new DefaultObjectWrapperBuilder(Configuration.getVersion())
				.build()
				.getStaticModels()
				.get(ResourceConstants.class.getName());
		}
		catch (TemplateModelException e) {
			throw new IllegalStateException("Could not generate template model for " + ResourceConstants.class.getName());
		}
	}
}
