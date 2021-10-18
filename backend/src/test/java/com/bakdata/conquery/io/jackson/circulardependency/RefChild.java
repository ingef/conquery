package com.bakdata.conquery.io.jackson.circulardependency;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(base = IChild.class, id = "REF_CHILD")
@Getter
@Setter
public class RefChild implements IChild {
	private String sampleRefProp;
	@JsonIgnore
	private Container parent;

	public RefChild() {
		log.info("Ref Child : Constructor call");
	}

	@Override
	public void simpleCommonMethod() {
		log.info("RefChild : SimpleCommonMethod call");
	}


	@Override
	public void addParent(Container container) {
		this.setParent(container);
		log.info("Setter called");
	}
}
