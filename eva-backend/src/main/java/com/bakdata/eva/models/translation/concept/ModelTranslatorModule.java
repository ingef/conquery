package com.bakdata.eva.models.translation.concept;

import java.io.File;

import com.bakdata.eva.models.translation.RelativeFileSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ModelTranslatorModule extends SimpleModule {

	public ModelTranslatorModule() {
		this.addSerializer(File.class, new RelativeFileSerializer());
	}
}
