package com.bakdata.conquery.models.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.Valid;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Dataset extends Labeled<DatasetId> implements Injectable {

	@JsonManagedReference @Valid
	private IdMap<TableId, Table> tables = new IdMap<>();
	
	@JsonIgnore @Valid
	private List<Concept<?>> concepts = new ArrayList<>();
	
	@JsonManagedReference(StructureNode.MANAGED_DATASET_STRUCTURE) @Valid
	private List<StructureNode> structureNodes = new ArrayList<>();
	
	@Override
	public MutableInjectableValues inject(MutableInjectableValues mutableInjectableValues) {
		return mutableInjectableValues.add(Dataset.class, this);
	}

	@Override
	public DatasetId createId() {
		return new DatasetId(getName());
	}

	public synchronized void addConcept(Concept<?> concept) {
		concepts.add(concept);
	}

	public Stream<StructureNode> streamAllStructureNodes() {
		return structureNodes
			.stream()
			.flatMap(StructureNode::streamAllChildren);
	}
}
