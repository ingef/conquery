package com.bakdata.conquery.models.query.queryplan.clone;

public interface CtxCloneable<SELF extends CtxCloneable<SELF>> extends Resetable{
	default SELF clone(CloneContext ctx) {
		return ctx.clone((SELF)this);
	}
	
	@Deprecated
	SELF doClone(CloneContext ctx);
}
