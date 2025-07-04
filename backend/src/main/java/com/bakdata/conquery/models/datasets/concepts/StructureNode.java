package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.identifiable.LabeledNamespaceIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper=true,of={"children"})
public class StructureNode extends LabeledNamespaceIdentifiable<StructureNodeId> {

	public static final String MANAGED_STRUCTURE_STRUCTURE = "structure_structure";
	public static final String MANAGED_DATASET_STRUCTURE = "dataset_structure";

	@NotNull
	private DatasetId dataset;
	private String description;
	@Valid @JsonManagedReference(MANAGED_STRUCTURE_STRUCTURE)
	private List<StructureNode> children = Collections.emptyList();
	@JsonBackReference(MANAGED_STRUCTURE_STRUCTURE)
	@EqualsAndHashCode.Exclude
	private StructureNode parent;
	@Getter
	private LinkedHashSet<ConceptId> containedRoots = new LinkedHashSet<>();
	private List<KeyValue> additionalInfos = Collections.emptyList();

	@Override
	public StructureNodeId createId() {
		return new StructureNodeId(dataset, parent != null ? parent.getId() : null, getName());
	}

	public Stream<StructureNode> stream() {
		return Stream.concat(Stream.of(this), children.stream());
	}
}
