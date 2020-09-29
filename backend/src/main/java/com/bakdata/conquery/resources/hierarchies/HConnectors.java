package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.google.common.collect.MoreCollectors;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}")
public abstract class HConnectors extends HConcepts {
	
	@PathParam(TABLE)
	protected TableId tableId;
	protected ConnectorId connectorId;
	protected Connector connector;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		try {
			connector = concept.getConnectors()
				.stream()
				.filter(con->con.getTable().getId().equals(tableId))
				.collect(MoreCollectors.toOptional())
				.orElseThrow(()->new NoSuchElementException("No connector of "+conceptId+" maps to table "+tableId));
			connectorId = connector.getId();
		}
		catch (NoSuchElementException e) {
			throw new WebApplicationException("Could not find connector "+connector+" in "+concept, e, Status.NOT_FOUND);
		}
	}
}