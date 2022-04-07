package com.bakdata.conquery.models.datasets.concepts.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public interface GroupFilter {

	/**
	 * Hook for the {@link com.bakdata.conquery.apiv1.query.concept.filter.FilterValue.GroupFilterDeserializer} to
	 * determine the type of the value container of the group filter.
	 *
	 * <pre>
	 *     &#64;CPSType(id = "TEST_GROUP_FILTER", base = Filter.class)
	 * 	   public class GroupFilter extends SingleColumnFilter<GroupContainer> implements GroupFilter {
	 * 	   ...
	 *         &#64;Override
	 *         public JavaType getFilterValueType(TypeFactory tf) {
	 *             return tf.constructSimpleType(GroupContainer.class, null);
	 *         }
	 *     }
	 * </pre>
	 *
	 * @param tf Jacksons type factory
	 * @return	The type describing the object to deserialize.
	 */
	JavaType getFilterValueType(TypeFactory tf);
}
