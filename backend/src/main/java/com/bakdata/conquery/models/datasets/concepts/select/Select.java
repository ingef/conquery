package com.bakdata.conquery.models.datasets.concepts.select;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.SelectHolder;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class Select extends Labeled<SelectId> implements NamespacedIdentifiable<SelectId> {

	@JsonBackReference @Getter @Setter
	private SelectHolder<?> holder;

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getHolder().findConcept().getDataset();
	}

	@Setter @Getter
	private String description;

	@Setter @Getter @JsonProperty("default")
	private boolean isDefault = false;
	
	@JsonIgnore @Getter(lazy=true)
	private final ResultType resultType = createAggregator().getResultType();

	public abstract Aggregator<?> createAggregator();

	@Override
	public SelectId createId() {
		if(holder instanceof Connector) {
			return new ConnectorSelectId(((Connector)holder).getId(), getName());
		}
		return new ConceptSelectId(holder.findConcept().getId(), getName());
	}


	@NotNull
	public void appendColumnName(StringBuilder sb) {
		if (getHolder() instanceof Connector && getHolder().findConcept().getConnectors().size() > 1) {
			// The select originates from a connector and the corresponding concept has more than one connector -> Print also the connector
			sb.append(((Connector) getHolder()).getLabel());
			sb.append(' ');
		}
		sb.append(getLabel());
	}
}
