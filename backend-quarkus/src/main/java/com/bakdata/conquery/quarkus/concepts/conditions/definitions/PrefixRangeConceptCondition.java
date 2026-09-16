package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataPrefixRangeConceptCondition", description = "Matches fixed-length prefixes inside an inclusive lexical range.")
@PolymorphicModelSubtype(base = ConceptCondition.class, id = "PREFIX_RANGE")
public final class PrefixRangeConceptCondition extends AbstractConceptCondition {

	@NotBlank
	private String min;

	@NotBlank
	private String max;

	@AssertTrue(message = "min and max must have equal lengths and min must be smaller than or equal to max")
	public boolean isValidRange() {
		return min == null || max == null || min.length() == max.length() && min.compareTo(max) <= 0;
	}
}
