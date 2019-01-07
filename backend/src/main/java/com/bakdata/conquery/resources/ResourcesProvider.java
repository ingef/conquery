package com.bakdata.conquery.resources;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.io.cps.CPSBase;

@CPSBase
public interface ResourcesProvider {

        void registerResources(MasterCommand master);
}
