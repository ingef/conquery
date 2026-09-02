package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.plugin.api.filters.SingleColumnFilterDefinition;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.quarkus.concepts.filters.StaticFrontendValue;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
public abstract class SelectFilterDefinition extends SingleColumnFilterDefinition {

	@Schema(description = "Inline mapping from stored values to labels displayed by the frontend.")
	private Map<String, String> labels;
	@Schema(description = "Inline frontend options. Takes precedence over labels when present.")
	private List<StaticFrontendValue> options;
}
