package com.bakdata.conquery.resources.admin.ui.model;

import java.nio.charset.StandardCharsets;

import io.dropwizard.views.View;
import lombok.Getter;

@Getter
public class UIView<CONTENT_TYPE> extends View {
	
	private final CONTENT_TYPE c;
	
	public UIView(String templateName) {
		this(resolve(templateName), null);
	}
	
	public UIView(String templateName, CONTENT_TYPE content) {
		super(resolve(templateName), StandardCharsets.UTF_8);
		this.c = content;
	}

	private static String resolve(String templateName) {
        return String.format("/com/bakdata/conquery/resources/admin/ui/%s", templateName);
	}
}
