package com.bakdata.conquery.io.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IId.Parser;

public class IdParamConverter<T extends IId<?>> implements ParamConverter<T> {
	
	private final Parser<T> parser;
	
	public IdParamConverter(Class<T> type) {
		parser = IId.createParser(type);
	}
	
	@Override
	public T fromString(String value) {
		return parser.parse(value);
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}
	
	public static enum Provider implements ParamConverterProvider {
		INSTANCE;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
			if(IId.class.isAssignableFrom(rawType)) {
				return new IdParamConverter(rawType);
			}
			return null;
		}
		
	}
}
