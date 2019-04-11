package com.bakdata.conquery.resources.admin.ui;

import java.util.List;

import com.bakdata.conquery.util.io.FileTreeReduction;

import lombok.Getter;

@Getter
public class FileView<CONTENT_TYPE> extends UIView<CONTENT_TYPE> {
	
	private final List<FileTreeReduction> files;

	public FileView(String templateName, UIContext ctx, CONTENT_TYPE content, List<FileTreeReduction> files) {
		super(templateName, ctx, content);
		this.files = files;
	}
}
