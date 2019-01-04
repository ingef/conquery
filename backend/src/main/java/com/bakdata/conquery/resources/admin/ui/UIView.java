package com.bakdata.conquery.resources.admin.ui;

import java.nio.charset.StandardCharsets;

import io.dropwizard.views.View;
import lombok.Getter;

@Getter
public class UIView<CONTENT_TYPE> extends View {
	
	private final UIContext ctx;
	private final CONTENT_TYPE c;
	
	public UIView(String templateName, UIContext ctx) {
		this(templateName, ctx, null);
	}
	
	public UIView(String templateName, UIContext ctx, CONTENT_TYPE content) {
		super(templateName, StandardCharsets.UTF_8);
		this.c = content;
		this.ctx = ctx;
	}
}
