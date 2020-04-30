package com.bakdata.conquery.models.query.concept.filter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
public class CQUnfilteredTable {
	@Valid @NotNull
	private ConnectorId id;
	@Valid
	private ValidityDateColumn dateColumn;

	@JsonIgnore
	private Connector resolvedConnector;
	
	@Data @AllArgsConstructor @NoArgsConstructor
	public static class ValidityDateColumn {
		private ValidityDateId value;
		
		public ValidityDateColumn(String id) {
			this(ValidityDateId.Parser.INSTANCE.parse(id));
		}
	}

	public String selectedValidityDate() {
		if(dateColumn == null || dateColumn.getValue() == null) {
			return null;
		}
		return dateColumn.getValue().getValidityDate();
	}
}
