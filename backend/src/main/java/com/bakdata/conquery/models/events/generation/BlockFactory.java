package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.Bucket;

public abstract class BlockFactory {

	public abstract Block createBlock(Import imp, List<Object[]> events);
	public abstract Block readBlock(Import imp, InputStream inputStream) throws IOException;
}
