package com.bakdata.conquery.io.cps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(CPSTypes.class)
public @interface CPSType {
	String id();
	Class<?> base();
	/**
	 * When true, it indicates that the class uses sub-typing information in its id, which is saved in objects. if so, the class must implement 
	 * {@link SubTyped}. The consumed and produced type ids used by serdes (Jackson) are of the form &lt;TYPE-ID&gt;{@link CPSTypeIdResolver#SEPARATOR_SUB_TYPE}&lt;SUBTYPE-ID&gt;.
	 */
	boolean subTyped() default false;
}