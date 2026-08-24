package com.bakdata.conquery.quarkus.testplugin;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.SingleColumnFilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataPluginPrefixFilter", description = "Filter contributed by the separate test plugin JAR.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "PLUGIN_PREFIX")
public final class PrefixFilterDefinition extends SingleColumnFilterDefinition {

	@NotBlank
	@Schema(description = "Prefix suggested by the plugin.", required = true)
	private String prefix;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
