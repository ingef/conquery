package com.bakdata.conquery.models.query.queryplan.clone;

public interface CtxCloneable<SELF extends CtxCloneable<SELF>> {
	SELF doClone(CloneContext ctx);
}
