package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}/filters/{" + FILTER + "}")
public abstract class HFilters extends HConnectors {
	
	@PathParam(FILTER)
	protected FilterId filterId;
	protected Filter<?> filter;
	
	@PostConstruct
	@Override
	public void init() {
		super.init();
		try {
			this.filter = getNamespace().getStorage().getCentralRegistry().resolve(filterId);
		}
		catch(NoSuchElementException e) {
			throw new WebApplicationException("Could not find connector "+connector+" in "+concept, e, Status.NOT_FOUND);
		}
	}
}