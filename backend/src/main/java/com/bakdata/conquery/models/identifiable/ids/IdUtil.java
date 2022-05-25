package com.bakdata.conquery.models.identifiable.ids;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@UtilityClass
public final class IdUtil {

	public static final char JOIN_CHAR = '.';
	public static final Joiner JOINER = Joiner.on(JOIN_CHAR);
	private static final Map<Class<?>, Class<?>> CLASS_TO_ID_MAP = new ConcurrentHashMap<>();

	public static <ID extends AId<?>> ID intern(ID id) {
		@SuppressWarnings("unchecked")
		ID old = IIdInterner.forParser((Parser<ID>) createParser(id.getClass())).putIfAbsent(id.collectComponents(), id);
		if (old == null) {
			return id;
		}
		checkConflict(id, old);
		return old;
	}

	public static <T extends AId<?>> Parser<T> createParser(Class<T> idClass) {
		return (Parser<T>) idClass.getDeclaredClasses()[0].getEnumConstants()[0];
	}

	public static void checkConflict(AId<?> id, AId<?> cached) {
		if (!cached.equals(id)) {
			throw new IllegalStateException("The cached id '"
											+ cached
											+ "'("
											+ cached.getClass().getSimpleName()
											+ ") conflicted with a new entry of "
											+ id.getClass().getSimpleName());
		}
	}

	public static <T extends AId<?>> Class<T> findIdClass(Class<?> cl) {
		Class<?> result = CLASS_TO_ID_MAP.get(cl);

		if (result != null) {
			return (Class<T>) result;
		}

		String methodName = "getId";
		if (IdentifiableImpl.class.isAssignableFrom(cl)) {
			methodName = "createId";
		}

		try {
			Class<?> returnType = MethodUtils.getAccessibleMethod(cl, methodName).getReturnType();
			if (!AId.class.isAssignableFrom(returnType) || AId.class.equals(returnType)) {
				throw new IllegalStateException("The type `" + returnType + "` of `" + cl + "#" + methodName + "` is not a specific subtype of IId");
			}

			if (NamespacedIdentifiable.class.isAssignableFrom(cl) != NamespacedId.class.isAssignableFrom(returnType)) {
				throw new IllegalStateException(String.format("%s and %s are not both Namespaced.", cl, returnType));
			}

			result = returnType;
			CLASS_TO_ID_MAP.put(cl, result);

			return (Class<T>) Objects.requireNonNull(result);
		}
		catch (SecurityException e1) {
			throw new IllegalStateException("The type " + cl + " has no " + methodName + " method", e1);
		}
	}

	public interface Parser<ID extends AId<?>> {

		default ID parse(String id) {
			return parse(split(id));
		}

		default ID parse(String... id) {
			return parse(Arrays.asList(id));
		}

		static String[] split(String id) {
			Objects.requireNonNull(id, "An empty id was provided for parsing.");
			String[] parts = StringUtils.split(id, IdUtil.JOIN_CHAR);
			for (int i = 0; i < parts.length; ++i) {
				parts[i] = ConqueryEscape.unescape(parts[i]);

			}
			return parts;
		}

		default ID parse(List<String> parts) {
			//first check if we get the result with the list (which might be a sublist)
			ID result = IIdInterner.forParser(this).get(parts);
			if (result == null) {
				result = createId(parts);
				//if not make a minimal list and use that to compute so that we do not keep the sublist
				ID secondResult = IIdInterner.forParser(this).putIfAbsent(ImmutableList.copyOf(parts), result);
				if (secondResult != null) {
					checkConflict(result, secondResult);
					return secondResult;
				}
			}
			return result;
		}

		default ID createId(List<String> parts) {
			IdIterator it = new IdIterator(parts);
			return checkNoRemaining(parseInternally(it), it, parts);
		}

		default ID checkNoRemaining(ID id, IdIterator remaining, List<String> allParts) {
			if (remaining.remaining() > 0) {
				throw new IllegalStateException(
						String.format(
								"After parsing '%s' as id '%s' of type %s there are parts remaining: '%s'",
								IdUtil.JOINER.join(allParts),
								id,
								id.getClass().getSimpleName(),
								IdUtil.JOINER.join(remaining.getRemaining())
						)
				);
			}
			return id;
		}

		ID parseInternally(IdIterator parts);

		default ID parse(IdIterator parts) {
			//first check if we get the result with the list (which might be a sublist)
			List<String> input = parts.getRemaining();
			ID result = IIdInterner.forParser(this).get(input);
			if (result == null) {
				parts.internNext();
				result = parseInternally(parts);
				//if not make a minimal list and use that to compute so that we do not keep the sublist
				ID secondResult = IIdInterner.forParser(this).putIfAbsent(ImmutableList.copyOf(input), result);
				if (secondResult != null) {
					checkConflict(result, secondResult);
					return secondResult;
				}
				return result;
			}
			parts.consumeAll();
			return result;
		}

		default ID parsePrefixed(String dataset, String id) {
			String[] result;
			String[] split = split(id);
			//if already prefixed
			if (split.length > 0 && split[0].equals(dataset)) {
				result = split;
			}
			else {
				result = new String[split.length + 1];
				result[0] = dataset;
				System.arraycopy(split, 0, result, 1, split.length);
			}
			return parse(Arrays.asList(result));
		}
	}
}