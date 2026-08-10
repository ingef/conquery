package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataSumSelect", description = "Sums a numeric column, optionally subtracting another column.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "SUM")
public final class SumSelectDefinition extends SingleColumnSelectDefinition {
	private String subtractColumn;
	private List<String> distinctByColumn;
}
