package com.bakdata.conquery.models.identifiable.ids;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import jersey.repackaged.com.google.common.collect.Lists;

@JsonDeserialize(using=IdDeserializer.class)
public interface IId<TYPE> {

	public static final char JOIN_CHAR = '.';
	public static final Joiner JOINER = Joiner.on(JOIN_CHAR);
	public static final Map<Class<?>, Class<?>> CLASS_TO_ID_MAP = new ConcurrentHashMap<>();
	static final Map<List<String>, IId<?>> INTERNED_IDS = new ConcurrentHashMap<>();
	
	public static interface Parser<ID extends IId<?>> {

		default ID parse(String id) {
			List<String> parts = ImmutableList.copyOf(StringUtils.split(id, IId.JOIN_CHAR));
			return parse(parts);
		}
		
		@SuppressWarnings("unchecked")
		default ID parse(List<String> parts) {
			return (ID)INTERNED_IDS.computeIfAbsent(parts, this::createId);
		}
		
		default ID createId(List<String> parts) {
			parts = ImmutableList.copyOf(Lists.transform(parts,String::intern));
			Iterator<String> it = parts.iterator();
			return checkNoRemaining(parse(it), it);
		}
		
		@SuppressWarnings("unchecked")
		default ID parse(String dataset, String id) {
			List<String> parts = new ImmutableList.Builder<String>()
				.add(dataset)
				.addAll(Iterators.forArray(StringUtils.split(id, IId.JOIN_CHAR)))
				.build();
			return (ID)INTERNED_IDS.computeIfAbsent(parts, this::createId);
		}
		
		ID parse(Iterator<String> parts);
		
		default ID checkNoRemaining(ID id, Iterator<String> parts) {
			if(parts.hasNext()) {
				throw new IllegalStateException(
					String.format(
						"Remaining parts '%s' after parsing '%s' as '%s'",
						IId.JOINER.join(parts),
						id,
						id.getClass().getSimpleName()
					)
				);
			}
			else {
				return id;
			}
		}
	}
	
	public static <T extends IId<?>> Class<T> findIdClass(Class<?> cl) {
		Class<?> result = CLASS_TO_ID_MAP.get(cl);
		if(result == null) {
			String methodName = "getId";
			if(IdentifiableImpl.class.isAssignableFrom(cl)) {
				methodName = "createId";
			}
			try {
				Class<?> returnType = MethodUtils.getAccessibleMethod(cl, methodName).getReturnType();
				try {
					if(AId.class.isAssignableFrom(returnType) && !AId.class.equals(returnType)) {
						result = returnType;
						CLASS_TO_ID_MAP.put(cl, result);
					}
					else {
						throw new IllegalStateException("The type "+returnType+" of "+methodName+" is not a specific subtype of IId");
					}
				} catch (SecurityException e) {
					throw new IllegalStateException("The type "+returnType+" has no parse method", e);
				}
			} catch (SecurityException e1) {
				throw new IllegalStateException("The type "+cl+" has no "+methodName+" method", e1);
			}
		}
		return (Class<T>)result;
	}
	
	//TODO these methods should be cached and more performant
	public static <T extends IId<?>> Parser<T> createParser(Class<T> idClass) {
		return (Parser<T>)idClass.getDeclaredClasses()[0].getEnumConstants()[0];
	}
}