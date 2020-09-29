package com.bakdata.conquery.resources;

import java.io.Closeable;
import java.io.IOException;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSBase;

@CPSBase
public interface ResourcesProvider extends Closeable {

	void registerResources(ManagerNode manager);
	
	@Override
	default void close() throws IOException {}
}
