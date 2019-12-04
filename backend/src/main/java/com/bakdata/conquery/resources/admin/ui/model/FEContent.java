package com.bakdata.conquery.resources.admin.ui.model;


import freemarker.template.TemplateModel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Container class for URI elements that are statically defined by Conquery.
 */
@Getter
@SuperBuilder
public abstract class FEContent {

	public TemplateModel staticUriElem;
}
