package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataPrefixSelect", description = "Returns values prefixed with a configured string.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "PREFIX")
public final class PrefixSelectDefinition extends SingleColumnSelectDefinition {
	@NotNull
	private String prefix;
}
