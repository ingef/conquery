package com.bakdata.conquery.io.cps;


/**
 * Interface that must be implemented by classes that are marked with {@link CPSType} and flagged that they carry sub-typing information.
 * The sub-typing information is only available at runtime and saved in the object.
 * {@link CPSTypeIdResolver#SEPARATOR_SUB_TYPE}
 * The consumed and produced type ids used by serdes (Jackson) are of the form &lt;TYPE-ID&gt;{@link CPSTypeIdResolver#SEPARATOR_SUB_TYPE}&lt;SUBTYPE-ID&gt;.
 */
public interface SubTyped {
	String getSubType();
}
