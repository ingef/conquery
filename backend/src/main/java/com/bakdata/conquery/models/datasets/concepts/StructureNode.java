package com.bakdata.conquery.models.datasets.concepts;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper=true,of={"children"})
public class StructureNode extends Labeled<StructureNodeId> {

	public static final String MANAGED_STRUCTURE_STRUCTURE = "structure_structure";
	public static final String MANAGED_DATASET_STRUCTURE = "dataset_structure";
	
	@NsIdRef @NotNull
	private Dataset dataset;
	private String description;
	@Valid @JsonManagedReference(MANAGED_STRUCTURE_STRUCTURE)
	private List<StructureNode> children = Collections.emptyList();
	@JsonBackReference(MANAGED_STRUCTURE_STRUCTURE)
	private StructureNode parent;
	@Getter
	private LinkedHashSet<ConceptId> containedRoots = new LinkedHashSet<>();
	private List<KeyValue> additionalInfos = Collections.emptyList();
	
	@Override
	public StructureNodeId createId() {
		return new StructureNodeId(dataset.getId(), parent!=null?parent.getId():null, getName());
	}
}
