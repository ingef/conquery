package com.bakdata.conquery.models.events.generation;

import java.io.InputStream;
import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Block;

public abstract class BlockFactory {

	public abstract Block createBlock(int entity, Import imp, List<Object[]> events);
	public abstract Block readBlock(int entity, Import imp, InputStream inputStream);
}
