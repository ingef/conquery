package com.bakdata.conquery.models.datasets.concepts;

import java.util.function.BiFunction;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
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
public class ValidityDate extends NamespacedIdentifiable<ValidityDateId> implements DaterangeSelectOrFilter {

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

	@JsonIgnore
	private BiFunction<Integer, Bucket, CDateRange> extractor;

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

	@CheckForNull
	public CDateRange getValidityDate(int event, Bucket bucket) {
		if (extractor == null){
			//TODO this is just a workaround: We should actually be using Initializing, which sadly gives us issues with LoadingUtil
			init();
		}

		return extractor.apply(event, bucket);
	}

	public boolean containsColumn(ColumnId id) {
		return id.equals(getColumn()) || id.equals(getStartColumn()) || id.equals(getEndColumn());
	}

	@JsonIgnore
	@ValidationMethod(message = "ValidityDate is not for Connectors' Table.")
	public boolean isForConnectorsTable() {

		final boolean anyColumnNotForConnector =
				(startColumn != null && !startColumn.getTable().equals(connector.getResolvedTable().getId()))
				|| (endColumn != null && !endColumn.getTable().equals(connector.getResolvedTable().getId()));

		final boolean columnNotForConnector = column != null && !column.getTable().equals(connector.getResolvedTableId());

		return !anyColumnNotForConnector && !columnNotForConnector;
	}

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return connector.getDataset();
	}

	@Override
	public ValidityDateId createId() {
		return new ValidityDateId(connector.getId(), getName());
	}

	public void init() {
		// Initialize extractor early to avoid resolve and dispatch in very hot code. Hopefully boxing can be elided.
		if (column != null) {
			final Column resolvedColumn = column.resolve();

			extractor = (event, bucket) -> {
				if (bucket.has(event, resolvedColumn)) {
					return bucket.getAsDateRange(event, resolvedColumn);
				}

				return null;
			};
			return;
		}

		final Column resolvedStartColumn = startColumn.resolve();
		final Column resolvedEndColumn = endColumn.resolve();

		extractor = (event, bucket) -> {
			final boolean hasStart = bucket.has(event, resolvedStartColumn);
			final boolean hasEnd = bucket.has(event, resolvedEndColumn);

			if (!hasStart && !hasEnd) {
				return null;
			}

			final int start = hasStart ? bucket.getDate(event, resolvedStartColumn) : Integer.MIN_VALUE;
			final int end = hasEnd ? bucket.getDate(event, resolvedEndColumn) : Integer.MAX_VALUE;

			return CDateRange.of(start, end);
		};
	}
}
