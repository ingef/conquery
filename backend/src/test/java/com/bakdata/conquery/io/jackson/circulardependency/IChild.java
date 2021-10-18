package com.bakdata.conquery.io.jackson.circulardependency;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface   IChild {


	 void  simpleCommonMethod();
	@JsonBackReference
	  void addParent(Container container);
}
