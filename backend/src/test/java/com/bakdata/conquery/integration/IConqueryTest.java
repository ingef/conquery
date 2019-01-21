package com.bakdata.conquery.integration;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.util.support.TestConquery;

@CPSBase
public interface IConqueryTest<T extends TestConquery> {

	void init(T conquery);
	void execute();
	void finish();
}
