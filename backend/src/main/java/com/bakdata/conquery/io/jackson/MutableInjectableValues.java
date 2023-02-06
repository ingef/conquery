package com.bakdata.conquery.io.jackson;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class MutableInjectableValues extends InjectableValues {

	private final ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();
	
	public <T> MutableInjectableValues add(Class<T> type, T value) {
		if(!type.isInstance(value)) {
			throw new IllegalArgumentException(value+" is not of type "+type);
		}
		values.put(type.getName(), value);
		return this;
	}

	@Override
	public Object findInjectableValue(Object valueId, DeserializationContext ctxt, BeanProperty forProperty, Object beanInstance) throws JsonMappingException {
		if(valueId instanceof Class) {
			return findInjectableValue(((Class) valueId).getName(), ctxt, forProperty, beanInstance);
		}
		if (!(valueId instanceof String)) {
			ctxt.reportBadDefinition(ClassUtil.classOf(valueId),
					String.format(
							"Unrecognized inject value id type (%s), expecting String or Class",
							ClassUtil.classNameOf(valueId)
					)
			);
		}
		String key = (String) valueId;
		return values.get(key);
	}

	public MutableInjectableValues copy() {
		MutableInjectableValues res = new MutableInjectableValues();
		res.values.putAll(values);
		return res;
	}

}
