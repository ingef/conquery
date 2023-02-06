package com.bakdata.conquery.models.datasets.concepts;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class ValidityDate extends Labeled<ValidityDateId> implements NamespacedIdentifiable<ValidityDateId> {

	@NsIdRef
	@NotNull
	private Column column;
	@JsonBackReference
	private Connector connector;

	@Override
	public ValidityDateId createId() {
		return new ValidityDateId(connector.getId(), getName());
	}

	@JsonIgnore
	@ValidationMethod(message = "Column is not of Date or DateRange.")
	public boolean isValidValidityDates() {
		if (getColumn().getType().isDateCompatible()) {
			return true;
		}

		log.error("ValidityDate-Column[{}] is not of type DATE or DATERANGE", getColumn().getId());
		return false;
	}

	@JsonIgnore
	@ValidationMethod(message = "ValidityDate is not for Connectors' Table.")
	public boolean isForConnectorsTable() {

		if (getColumn().getTable().equals(connector.getTable())) {
			return true;
		}

		log.error("ValidityDate[{}](Column = `{}`) does not belong to Connector[{}]#Table[{}]", getId(), getColumn().getId(), getId(), connector.getTable().getId());

		return false;
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return connector.getDataset();
	}
}