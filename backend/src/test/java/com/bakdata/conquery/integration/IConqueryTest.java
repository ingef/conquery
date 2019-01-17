package com.bakdata.conquery.integration;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.util.support.TestConquery;

@CPSBase
public interface IConqueryTest {

	void init(TestConquery conquery);
	void execute();
	void finish();
}
