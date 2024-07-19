package com.bakdata.conquery.util.extentions;

import jakarta.validation.Validator;

import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.models.config.ConqueryConfig;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class InternalObjectMapperCreatorExtention implements BeforeAllCallback {
	private final InternalObjectMapperCreator creator;

	public InternalObjectMapperCreatorExtention(ConqueryConfig config, Validator validator) {
		creator = new InternalObjectMapperCreator(config, validator);
	}

	@Override
	public void beforeAll(ExtensionContext extensionContext) throws Exception {

	}
}
