package com.bakdata.conquery.resources;

import java.io.Closeable;
import java.io.IOException;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSBase;

@CPSBase
public interface ResourcesProvider extends Closeable {

	void registerResources(MasterCommand master);
	
	@Override
	default void close() throws IOException {}
}
