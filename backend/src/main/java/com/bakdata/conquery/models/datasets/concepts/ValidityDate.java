package com.bakdata.conquery.models.datasets.concepts;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class ValidityDate extends Labeled<ValidityDateId> implements NamespacedIdentifiable<ValidityDateId> {

	@NsIdRef
	@Nullable
	private Column column;
	@NsIdRef
	@Nullable
	private Column startColumn;
	@NsIdRef
	@Nullable
	private Column endColumn;
	@JsonBackReference
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Connector connector;

	public static ValidityDate create(Column column) {
		final ValidityDate validityDate = new ValidityDate();
		validityDate.setColumn(column);
		return validityDate;
	}

	public static ValidityDate create(Column startColumn, Column endColumn) {
		final ValidityDate validityDate = new ValidityDate();
		validityDate.setColumn(startColumn);
		validityDate.setColumn(endColumn);
		return validityDate;
	}

	@Override
	public ValidityDateId createId() {
		return new ValidityDateId(connector.getId(), getName());
	}

	@CheckForNull
	public CDateRange getValidityDate(int event, Bucket bucket) {
		// I spent a lot of time trying to create two classes implementing single/multi-column valditiy dates separately.
		// JsonCreator was not happy and I could not figure out why. This is probably the most performant implementation that's not two classes.

		if (getColumn() != null) {
			if (bucket.has(event, getColumn())) {
				return bucket.getAsDateRange(event, getColumn());
			}

			return null;
		}

		final Column startColumn = getStartColumn();
		final Column endColumn = getEndColumn();

		final int start = bucket.has(event, startColumn) ? bucket.getDate(event, startColumn) : Integer.MIN_VALUE;
		final int end = bucket.has(event, endColumn) ? bucket.getDate(event, endColumn) : Integer.MAX_VALUE;

		if (start == Integer.MIN_VALUE && end == Integer.MAX_VALUE) {
			return null;
		}

		return CDateRange.of(start, end);

	}

	@JsonIgnore
	@ValidationMethod(message = "ValidityDate is not for Connectors' Table.")
	public boolean isForConnectorsTable() {

		final boolean
				anyColumnNotForConnector =
				(startColumn != null && !startColumn.getTable().equals(connector.getTable())) || (endColumn != null && !endColumn.getTable()
																																 .equals(connector.getTable()));

		final boolean columnNotForConnector = column != null && !column.getTable().equals(connector.getTable());

		return !anyColumnNotForConnector && !columnNotForConnector;
	}

	@JsonIgnore
	@ValidationMethod(message = "Single column date range (set via column) and two column date range (set via startColumn and endColumn) are exclusive.")
	public boolean isExclusiveValidityDates() {
		return (column == null && startColumn != null && endColumn != null) || (column != null && startColumn == null && endColumn == null);
	}

	@JsonIgnore
	@ValidationMethod(message = "Both columns of a two-column validity date have to be of type DATE.")
	public boolean isValidTwoColumnValidityDates() {
		if (startColumn == null || endColumn == null) {
			return true;
		}
		return startColumn.getType() == MajorTypeId.DATE && endColumn.getType() == MajorTypeId.DATE;
	}

	@JsonIgnore
	@ValidationMethod(message = "Column is not of type DATE or DATE_RANGE.")
	public boolean isValidValidityDatesSingleColumn() {
		if (column == null) {
			return true;
		}
		return column.getType().isDateCompatible();
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return connector.getDataset();
	}

}
