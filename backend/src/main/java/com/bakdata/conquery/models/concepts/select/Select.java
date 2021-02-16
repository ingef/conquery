package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.SelectHolder;
import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.identifiable.Labeled;
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

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class Select extends Labeled<SelectId> {

	@JsonBackReference @Getter @Setter
	private SelectHolder<?> holder;

	@Setter @Getter
	private String description;

	@Setter @Getter @JsonProperty("default")
	private boolean isDefault = false;
	
	@JsonIgnore @Getter(lazy=true)
	private final SimpleResultType resultType = createAggregator().getResultType();

	public abstract Aggregator<?> createAggregator();

	@Override
	public SelectId createId() {
		if(holder instanceof Connector) {
			return new ConnectorSelectId(((Connector)holder).getId(), getName());
		}
		return new ConceptSelectId(holder.findConcept().getId(), getName());
	}
}
