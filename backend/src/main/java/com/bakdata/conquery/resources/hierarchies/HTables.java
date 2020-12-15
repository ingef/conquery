package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import javassist.NotFoundException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Getter @Setter
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
public abstract class HTables extends HDatasets {
	
	@PathParam(TABLE)
	protected TableId tableId;
	protected Table table;

	@SneakyThrows({NotFoundException.class})
	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.table = namespace
			.getStorage()
			.getTable(tableId);

		if(table == null){
			throw new NotFoundException("Could not find Table " + tableId.toString());
		}
	}
}