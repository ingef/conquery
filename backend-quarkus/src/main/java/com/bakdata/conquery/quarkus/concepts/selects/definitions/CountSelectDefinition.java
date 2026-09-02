package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataCountSelect", description = "Counts matching values or events.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "COUNT")
public final class CountSelectDefinition extends AbstractSelectDefinition {
	private boolean distinct;
	private List<String> distinctByColumn;
	@NotBlank
	private String column;
}
