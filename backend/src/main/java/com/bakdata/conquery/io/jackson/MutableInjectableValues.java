package com.bakdata.conquery.io.jackson;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class MutableInjectableValues extends InjectableValues {

	private final ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();


	public <T> MutableInjectableValues add(Class<? extends T> type, T value) {
		if (!type.isInstance(value)) {
			throw new IllegalArgumentException("%s is not of type %s".formatted(value, type));
		}
		values.put(type.getName(), value);
		return this;
	}

	@Override
	public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) throws JsonMappingException {
		if (valueId instanceof Class<?> clazz) {
			return findInjectableValue(clazz.getName(), ctxt, forProperty, beanInstance);
		}
		else if (valueId instanceof String key) {
			return values.get(key);
		}
		ctxt.reportBadDefinition(ClassUtil.classOf(valueId),
								 String.format(
										 "Unrecognized inject value id type (%s), expecting String or Class",
										 ClassUtil.classNameOf(valueId)
								 )
		);

		return null; // Not reached
	}

	public MutableInjectableValues copy() {
		MutableInjectableValues res = new MutableInjectableValues();
		res.values.putAll(values);
		return res;
	}

}
