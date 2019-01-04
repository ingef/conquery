package com.bakdata.conquery.models.concepts;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This is a single node or concept in a concept tree.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@ToString(of={"name", "connectors"})
public abstract class Concept<CONNECTOR extends Connector> extends ConceptElement<ConceptId> {
	
	@Getter @Setter
	private boolean hidden = false;
	@JsonManagedReference @Valid @Getter @Setter
	private List<CONNECTOR> connectors=Collections.emptyList();
	@JsonIgnore @Setter @Getter
	private StructureNode structureParent;
	@NotNull @Getter @Setter
	private DatasetId dataset;
	
	public CONNECTOR getConnectorByName(String connector) {
		return connectors
				.stream()
				.filter(n->n.getName().equals(connector))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Connector not found: " + connector));
	}

	public void initElements(Validator validator) throws ConfigurationException, JSONException {}
	
	@Override @JsonIgnore
	public Concept<?> getConcept() {
		return this;
	}
	
	@Override
	public ConceptId createId() {
		return new ConceptId(Objects.requireNonNull(dataset), getName());
	}
}
