package com.bakdata.conquery.models.datasets.concepts.filters;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CompoundValueConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CompoundValueContainer;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FEFilterProcessor {
	public static <V extends CompoundValueContainer> Map<String, FEFilter> generateFrom(Filter<V> filter, Class<V> valueClass)
			throws ConceptConfigurationException {
		final HashMap<String, FEFilter> map = new HashMap<>();

		final Map<String, Method>
				valueConfigurations =
				Arrays.stream(filter.getClass().getMethods())
					  .filter(m -> m.isAnnotationPresent(CompoundValueConfig.class))
					  .collect(Collectors.toMap(m -> m.getAnnotation(CompoundValueConfig.class).value(), Function.identity()));

		for (Field field : valueClass.getDeclaredFields()) {
			field.setAccessible(true);
			if (field.isAnnotationPresent(CompoundValue.class)) {
				final Method method = valueConfigurations.get(field.getAnnotation(CompoundValue.class).value());
				if (method == null) {
					throw new ConceptConfigurationException(filter, "Unable to find frontend filter configuration for: " + field.getName());
				}
				try {

					final FEFilter config = (FEFilter) method.invoke(filter);
					map.put(field.getName(), config);
				}
				catch (InvocationTargetException | IllegalAccessException e) {
					throw new ConceptConfigurationException(filter, "Unable to create frontend filter configuration for: " + field.getName(), e);
				}

			}
		}
		return map;
	}
}
