package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataDurationSumSelect", description = "Sums durations of selected date ranges.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "DURATION_SUM")
public final class DurationSumSelectDefinition extends DateRangeSelectDefinition {
	private List<String> distinctBy;
}
