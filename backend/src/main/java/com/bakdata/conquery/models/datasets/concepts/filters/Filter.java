package com.bakdata.conquery.models.datasets.concepts.filters;

import java.util.List;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.LabeledNamespaceIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = true)
public abstract class Filter<FILTER_VALUE> extends LabeledNamespaceIdentifiable<FilterId> {

	private String unit;
	@JsonAlias("description")
	private String tooltip;
	@JsonBackReference
	@EqualsAndHashCode.Exclude
	private Connector connector;
	private String pattern;
	private Boolean allowDropFile;

	private FILTER_VALUE defaultValue;

	@JsonIgnore
	@Override
	public DatasetId getDataset() {
		return getConnector().getDataset();
	}

	public FrontendFilterConfiguration.Top createFrontendConfig(ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		final FrontendFilterConfiguration.Top f = FrontendFilterConfiguration.Top.builder()
																				 .id(getId())
																				 .label(getLabel())
																				 .tooltip(getTooltip())
																				 .unit(getUnit())
																				 .allowDropFile(getAllowDropFile())
																				 .pattern(getPattern())
																				 .defaultValue(getDefaultValue())
																				 .build();
		configureFrontend(f, conqueryConfig);
		return f;
	}

	protected abstract void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException;

	public abstract FilterNode<?> createFilterNode(FILTER_VALUE filterValue);

	@JsonIgnore
	@ValidationMethod(message = "Not all Filters are for Connector's table.")
	public boolean isForConnectorsTable() {
		boolean valid = true;

		for (ColumnId column : getRequiredColumns()) {
			TableId tableId = connector.getTableId();
			if (column == null || column.getTable().equals(tableId)) {
				continue;
			}

			log.error("Filter[{}] of Table[{}] is not of Connector[{}]#Table[{}]", getId(), column.getTable(), connector.getId(), tableId);

			valid = false;
		}

		return valid;
	}

	@JsonIgnore
	public abstract List<ColumnId> getRequiredColumns();

	@JsonIgnore
	public <F extends Filter<FILTER_VALUE>> FilterConverter<F, FILTER_VALUE> createConverter() {
		throw new UnsupportedOperationException("No converter implemented for Filter %s".formatted(getClass()));
	}

	@Override
	public FilterId createId() {
		return new FilterId(connector.getId(), getName());
	}
}
