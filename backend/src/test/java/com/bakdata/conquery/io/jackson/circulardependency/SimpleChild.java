package com.bakdata.conquery.io.jackson.circulardependency;


import com.bakdata.conquery.io.cps.CPSType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@CPSType(base = IChild.class, id = "SIMPLE")
public class SimpleChild implements IChild {

	public SimpleChild() {
		log.info("Simple Child : Constructor call");
	}

	@Override
	public void simpleCommonMethod() {
		log.info("Simple Child : SimpleCommonMethod");
	}

	@Override
	public void addParent(Container container) {

	}
}
