package com.bakdata.conquery.models.query;

import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Container class for the query API provide meta data for reach column in the
 * csv result. This can be used for the result preview in the frontend.
 */
@Data
@Builder
@AllArgsConstructor
public class ColumnDescriptor {

	/**
	 * The name of the column. This label should be generated as a unique label among the columns.
	 */
	private String label;

	private String description;

	/**
	 * If this descriptor originates from a {@link Select} which is a child of {@link CQConcept},
	 * it is the label of the corresponding {@link com.bakdata.conquery.models.datasets.concepts.Concept},
	 * otherwise <code>null</code>.
	 * <p>
	 * Beware that this label must not be unique among the columns
	 */
	private String defaultLabel;

	/**
	 * The datatype that corresponds to this column.
	 */
	private String type;

	private Set<SemanticType> semantics;

}
