package com.bakdata.conquery.models.query.concept.filter;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
public class CQTable {
	@Valid @NotNull
	private ConnectorId id;
	@Valid
	private ValidityDateColumn dateColumn;
	@Valid @NotNull
	private List<FilterValue<?>> filters = Collections.emptyList();

	@Valid @NotNull @NsIdRefCollection
	private List<Select> selects = Collections.emptyList();

	@JsonBackReference
	private CQConcept concept;

	@JsonIgnore
	private Connector resolvedConnector;
	
	@Data @AllArgsConstructor @NoArgsConstructor
	public static class ValidityDateColumn {
		private ValidityDateId value;
	}

	public String selectedValidityDate() {
		if(dateColumn == null || dateColumn.getValue() == null) {
			return null;
		}
		return dateColumn.getValue().getValidityDate();
	}
}
