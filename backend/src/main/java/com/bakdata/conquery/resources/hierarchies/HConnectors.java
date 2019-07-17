package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE_NAME;

import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}/tables/{" + TABLE_NAME + "}")
public abstract class HConnectors extends HConcepts {
	
	@PathParam(TABLE_NAME)
	protected ConnectorId connectorId;
	protected Connector connector;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		try {
			this.connector = concept.getConnectorByName(connectorId.getConnector());
		}
		catch (NoSuchElementException e) {
			throw new WebApplicationException("Could not find connector "+connector+" in "+concept, e, Status.NOT_FOUND);
		}
	}
}