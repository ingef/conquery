package com.bakdata.conquery.models.index;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface IndexKey<I extends Index<? extends IndexKey<I,V>,V>,V> {
	URL getCsv();
	String getInternalColumn();
	List<String> getExternalTemplates();

	I createIndex();
}
