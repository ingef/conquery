package com.bakdata.conquery.resources.admin.ui.model;

import java.nio.charset.StandardCharsets;

import io.dropwizard.views.common.View;
import lombok.Getter;

@Getter
public class UIView extends View {
	
	private final UIContext ctx;
	private final Object c;
	
	public UIView(String templateName, UIContext ctx) {
		this(templateName, ctx, null);
	}
	
	public UIView(String templateName, UIContext ctx, Object content) {
		super(resolve(templateName), StandardCharsets.UTF_8);
		this.c = content;
		this.ctx = ctx;
	}

	private static String resolve(String templateName) {
        return String.format("/com/bakdata/conquery/resources/admin/ui/%s", templateName);
	}
}
