package com.bakdata.conquery.models.concepts;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
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
	@NotNull @Getter @Setter
	private DatasetId dataset;

	public List<Select> getDefaultSelects() {
		return getSelects()
					  .stream()
					  .filter(Select::isDefault)
					  .collect(Collectors.toList());
	}

	public CONNECTOR getConnector(ConnectorId connectorId) {
		return connectors.stream()
						 .filter(conn -> connectorId.equals(conn.getId()))
						 .findAny()
						 .orElseThrow(() -> new NoSuchElementException("Connector not found: " + connectorId));
	}

	public abstract List<? extends Select> getSelects();
	
	public void initElements(Validator validator) throws ConfigurationException, JSONException {}
	
	@Override @JsonIgnore
	public Concept<?> getConcept() {
		return this;
	}
	
	@Override
	public ConceptId createId() {
		return new ConceptId(Objects.requireNonNull(dataset), getName());
	}
	
	public int countElements() {
		return 1;
	}
	
	@Override
	public long calculateBitMask() {
		return 0L;
	}
}
