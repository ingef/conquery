package com.bakdata.conquery.models.identifiable.ids;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

@JsonDeserialize(using=IdDeserializer.class)
public interface IId<TYPE> {

	char JOIN_CHAR = '.';
	Joiner JOINER = Joiner.on(JOIN_CHAR);
	Map<Class<?>, Class<?>> CLASS_TO_ID_MAP = new ConcurrentHashMap<>();
	Map<List<String>, IId<?>> INTERNED_IDS = new ConcurrentHashMap<>();
	
	interface Parser<ID extends IId<?>> {
		
		static String[] split(String id) {
			String[] parts = StringUtils.split(id, IId.JOIN_CHAR);
			for(int i = 0; i < parts.length; ++i){
				parts[i] = ConqueryEscape.unescape(parts[i]);
				
			}
			return parts;
		}

		default ID parse(String id) {
			return parse(Arrays.asList(split(id)));
		}
		
		@SuppressWarnings("unchecked")
		default ID parse(List<String> parts) {
			return (ID)INTERNED_IDS.computeIfAbsent(parts, this::createId);
		}
		
		default ID createId(List<String> parts) {
			parts = ImmutableList.copyOf(Lists.transform(parts,String::intern));
			PeekingIterator<String> it = Iterators.peekingIterator(parts.iterator());
			return checkNoRemaining(parse(it), it, parts);
		}
		
		default ID parsePrefixed(String dataset, String id) {
			String[] split = split(id);
			//if already prefixed
			if(split.length > 0 && split[0].equals(dataset)) {
				return parse(ImmutableList.copyOf(split));
			}

			List<String> parts = ImmutableList.<String>builderWithExpectedSize(split.length+1)
				.add(dataset)
				.add(split)
				.build();
			return parse(parts);
		}
		
		ID parse(PeekingIterator<String> parts);
		
		default ID checkNoRemaining(ID id, Iterator<String> remaining, List<String> allParts) {
			if(remaining.hasNext()) {
				throw new IllegalStateException(
					String.format(
						"After parsing '%s' as id '%s' of type %s there are parts remaining: '%s'",
						IId.JOINER.join(allParts),
						id,
						id.getClass().getSimpleName(),
						IId.JOINER.join(remaining)
					)
				);
			}
			else {
				return id;
			}
		}
	}
	
	static <T extends IId<?>> Class<T> findIdClass(Class<?> cl) {
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
	
	static <T extends IId<?>> Parser<T> createParser(Class<T> idClass) {
		return (Parser<T>)idClass.getDeclaredClasses()[0].getEnumConstants()[0];
	}
}