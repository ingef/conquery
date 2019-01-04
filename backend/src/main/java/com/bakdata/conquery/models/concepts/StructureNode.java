package com.bakdata.conquery.models.concepts;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import com.bakdata.conquery.models.common.KeyValue;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.StructureNodeId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper=true,of={"children"})
public class StructureNode extends Labeled<StructureNodeId> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String description;
	@Valid @JsonManagedReference
	private List<StructureNode> children = Collections.emptyList();
	@JsonBackReference
	private StructureNode parent;
	private List<String> containedRoots = Collections.emptyList();
	@JsonIgnore
	private List<Concept<?>> resolvedContained = Collections.emptyList();
	private List<KeyValue> additionalInfos = Collections.emptyList();
	
	@Override
	public StructureNodeId createId() {
		return new StructureNodeId(parent.getId(), getName());
	}
}
