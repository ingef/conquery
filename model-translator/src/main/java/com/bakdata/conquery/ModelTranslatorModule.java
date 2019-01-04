package com.bakdata.conquery;

import java.io.File;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class ModelTranslatorModule extends SimpleModule {

	public ModelTranslatorModule() {
		this.addSerializer(File.class, new RelativeFileSerializer());
	}
}
