package com.bakdata.conquery.integration;

import com.bakdata.conquery.util.support.TestConquery;

public interface IConqueryTestGen <T extends TestConquery> {

	void init(T conquery);
	void execute();
	void finish();
}
