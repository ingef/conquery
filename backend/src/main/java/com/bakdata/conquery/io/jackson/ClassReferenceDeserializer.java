package com.bakdata.conquery.io.jackson;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

public class ClassReferenceDeserializer extends StdScalarDeserializer<Object> implements ContextualDeserializer {

	private static final long serialVersionUID = 1L;
	private JavaType expectedType;

	protected ClassReferenceDeserializer() {
		super(String.class);
	}

	public ClassReferenceDeserializer(JavaType type) {
		super(String.class);
		this.expectedType = type;
	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String fullyQualifiedClassName = p.getValueAsString();
		try {
			Class<?> result = ctxt.findClass(fullyQualifiedClassName);
			if(expectedType!=null) {
				if(expectedType.hasGenericTypes()) {
					if(!expectedType.getBindings().getBoundType(0).getRawClass().isAssignableFrom(result)) {
						throw new JsonParseException(p, "The class "+fullyQualifiedClassName+" is not of type "+expectedType.getBindings().getBoundType(0));
					}
				}
			}
			return result.getConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			throw new JsonParseException(p, "Could not deserialize "+fullyQualifiedClassName, e);
		} catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException|NoSuchMethodException|SecurityException e) {
			throw new JsonParseException(p, "Could not instantiate "+fullyQualifiedClassName, e);
		}
		
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		return new ClassReferenceDeserializer(property.getType());
	}

}
