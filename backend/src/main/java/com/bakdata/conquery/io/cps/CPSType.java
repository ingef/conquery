package com.bakdata.conquery.io.cps;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(CPSTypes.class)
public @interface CPSType {
	public String id();
	public Class<?> base();
}