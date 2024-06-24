package com.bakdata.conquery.models.datasets.concepts;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ValidityDateId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class ValidityDate extends Labeled<ValidityDateId> implements NamespacedIdentifiable<ValidityDateId>, DaterangeSelectOrFilter {

	@Nullable
	private ColumnId column;
	@Nullable
	private ColumnId startColumn;
	@Nullable
	private ColumnId endColumn;
	@JsonBackReference
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Connector connector;

	public static ValidityDate create(Column column) {
		final ValidityDate validityDate = new ValidityDate();
		validityDate.setColumn(column.getId());
		return validityDate;
	}

	public static ValidityDate create(Column startColumn, Column endColumn) {
		final ValidityDate validityDate = new ValidityDate();
		validityDate.setColumn(startColumn.getId());
		validityDate.setColumn(endColumn.getId());
		return validityDate;
	}

	@Override
	public ValidityDateId createId() {
		return new ValidityDateId(connector.getId(), getName());
	}

	@CheckForNull
	public CDateRange getValidityDate(int event, Bucket bucket) {
		// I spent a lot of time trying to create two classes implementing single/multi-column valditiy dates separately.
		// JsonCreator was not happy, and I could not figure out why. This is probably the most performant implementation that's not two classes.

		if (getColumn() != null) {
			final Column resolvedColumn = getColumn().resolve();
			if (bucket.has(event, resolvedColumn)) {
				return bucket.getAsDateRange(event, resolvedColumn);
			}

			return null;
		}

		final Column startColumn = getStartColumn() != null ? getStartColumn().resolve() : null;
		final Column endColumn = getEndColumn() != null ? getEndColumn().resolve() : null;

		final boolean hasStart = bucket.has(event, startColumn);
		final boolean hasEnd = bucket.has(event, endColumn);

		if (!hasStart && !hasEnd) {
			return null;
		}

		final int start = hasStart ? bucket.getDate(event, startColumn) : Integer.MIN_VALUE;
		final int end = hasEnd ? bucket.getDate(event, endColumn) : Integer.MAX_VALUE;

		return CDateRange.of(start, end);
	}

	// TODO use Id as parameter
	public boolean containsColumn(Column column) {
		final ColumnId id = column.getId();
		return id.equals(getColumn()) || id.equals(getStartColumn()) || id.equals(getEndColumn());
	}

	@JsonIgnore
	@ValidationMethod(message = "ValidityDate is not for Connectors' Table.")
	public boolean isForConnectorsTable() {

		final boolean anyColumnNotForConnector =
				(startColumn != null && !startColumn.getTable().equals(connector.getResolvedTable().getId()))
				|| (endColumn != null && !endColumn.getTable().equals(connector.getResolvedTable().getId()));

		final boolean columnNotForConnector = column != null && !column.getTable().equals(connector.getResolvedTable().getId());

		return !anyColumnNotForConnector && !columnNotForConnector;
	}

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

}
