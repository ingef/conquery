package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.google.common.collect.MoreCollectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}/tables/{" + TABLE + "}")
public abstract class HConnectors extends HConcepts {

	@PathParam(TABLE)
	protected TableId table;
	protected Connector connector;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		connector = concept.getConnectors()
						   .stream()
						   .filter(con -> con.getResolvedTableId().equals(table))
						   .collect(MoreCollectors.toOptional())
						   .orElseThrow(() -> new NotFoundException(String.format("Could not find Connector for Table[%s] in Concept[%s]", connector, concept)));
	}
}