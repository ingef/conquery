package com.bakdata.conquery.models.datasets.concepts.select;

import java.util.List;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.SelectHolder;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@Slf4j
@EqualsAndHashCode(callSuper = true)
public abstract class Select extends Labeled<SelectId> implements NamespacedIdentifiable<SelectId> {

	@EqualsAndHashCode.Exclude
	@JsonBackReference
	@Getter
	@Setter
	private SelectHolder<?> holder;

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getHolder().findConcept().getDataset();
	}

	@Setter
	@Getter
	private String description;

	/**
	 * When set, the Frontend will preselect the Select for the User.
	 */
	@Setter @Getter @JsonProperty("default")
	private boolean isDefault = false;

	@JsonIgnore
	public abstract List<Column> getRequiredColumns();

	@JsonIgnore @Getter(lazy=true)
	private final ResultType resultType = createAggregator().getResultType();

	public abstract Aggregator<?> createAggregator();

	@Override
	public SelectId createId() {
		if (holder instanceof Connector) {
			return new ConnectorSelectId(((Connector) holder).getId(), getName());
		}
		return new ConceptSelectId(holder.findConcept().getId(), getName());
	}

	public void init() {
	}

	@NotNull
	@JsonIgnore
	public String getColumnName() {
		if (!(getHolder() instanceof Connector) || getHolder().findConcept().getConnectors().size() <= 1) {
			// The select belongs to a concept or a lone connector of a concept: just return the label
			return getLabel();
		}
		// The select originates from a connector and the corresponding concept has more than one connector -> Print also the connector
		return ((Connector) getHolder()).getLabel()
			   + ' '
			   + getLabel();
	}

	public SelectResultInfo getResultInfo(CQConcept cqConcept) {
		return new SelectResultInfo(this, cqConcept);
	}


	@JsonIgnore
	@ValidationMethod(message = "Select is not for Connector or not universal.")
	public boolean isForConnectorsTable() {
		boolean valid = true;

		if(holder instanceof Concept){
			return getRequiredColumns().isEmpty();
		}

		final Connector connector = (Connector) holder;

		for (Column column : getRequiredColumns()) {

			if (column == null || column.getTable() == connector.getTable()) {
				continue;
			}

			log.error("Select[{}] of Table[{}] is not of Connector[{}]#Table[{}]", getId(), column.getTable().getId(), connector.getId(), connector.getTable().getId());

			valid = false;
		}

		return valid;
	}
}
