package com.bakdata.conquery.models.query.queryplan.clone;

public interface CtxCloneable<SELF extends CtxCloneable<SELF>> {
	default SELF clone(CloneContext ctx) {
		return ctx.clone((SELF)this);
	}
	
	SELF doClone(CloneContext ctx);
}
