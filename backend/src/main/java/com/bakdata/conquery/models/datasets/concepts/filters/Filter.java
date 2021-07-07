package com.bakdata.conquery.models.datasets.concepts.filters;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is the abstract superclass for all filters.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@Slf4j
public abstract class Filter<FE_TYPE> extends Labeled<FilterId> implements NamespacedIdentifiable<FilterId> {

	private String unit;
	private String description;
	@JsonBackReference
	private Connector connector;
	private String pattern;
	private Boolean allowDropFile;

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getConnector().getDataset();
	}

	public abstract void configureFrontend(FEFilter f) throws ConceptConfigurationException;

	@JsonIgnore
	public abstract Column[] getRequiredColumns();

	public abstract FilterNode<?> createFilterNode(FE_TYPE filterValue);

	@Override
	public FilterId createId() {
		return new FilterId(connector.getId(), getName());
	}

	/**
	 * This method is called once at startup or if the dataset changes for each new import that
	 * concerns this filter. Use this to collect metadata from the import. It is not guaranteed that
	 * any blocks or cBlocks exist at this time. Any data created by this method should be volatile
	 * and @JsonIgnore.
	 *
	 * @param imp the import added
	 */
	public void addImport(Import imp) {
	}

	@JsonIgnore
	@ValidationMethod(message = "Not all Filters are for Connector's table.")
	public boolean isForConnectorsTable() {
		boolean valid = true;

		for (Column column : getRequiredColumns()) {
			if (column == null || column.getTable() == connector.getTable()) {
				continue;
			}

			log.error("Filter[{}] of Table[{}] is not of Connector[{}]#Table[{}]", getId(), column.getTable().getId(), connector.getId(), connector.getTable().getId());

			valid = false;
		}

		return valid;
	}
}
