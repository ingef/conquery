package com.bakdata.conquery.models.identifiable.ids;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.util.ConqueryEscape;
import com.google.common.base.Joiner;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@UtilityClass
public final class IdUtil {

	public static final char JOIN_CHAR = '.';
	public static final Joiner JOINER = Joiner.on(JOIN_CHAR);
	private static final Map<Class<?>, Class<?>> CLASS_TO_ID_MAP = new ConcurrentHashMap<>();

	public static <T extends Id<?>> Parser<T> createParser(Class<T> idClass) {
		return (Parser<T>) idClass.getDeclaredClasses()[0].getEnumConstants()[0];
	}

	public static <T extends Id<?>> Class<T> findIdClass(Class<?> cl) {
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
			if (!Id.class.isAssignableFrom(returnType) || Id.class.equals(returnType)) {
				throw new IllegalStateException("The type `" + returnType + "` of `" + cl + "#" + methodName + "` is not a specific subtype of Id");
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

	public interface Parser<ID extends Id<?>> {

		static List<String> asComponents(String id) {
			return Arrays.asList(split(id));
		}

		static String[] split(String id) {
			Objects.requireNonNull(id, "An empty id was provided for parsing.");
			String[] parts = StringUtils.split(id, IdUtil.JOIN_CHAR);
			for (int i = 0; i < parts.length; ++i) {
				parts[i] = ConqueryEscape.unescape(parts[i]);

			}
			return parts;
		}

		default ID parse(String id) {
			return parse(split(id));
		}

		default ID parse(String... id) {
			return parse(Arrays.asList(id));
		}

		default ID parse(List<String> parts) {
			//first check if we get the result with the list (which might be a sublist)
			ID result = createId(parts);
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

			parts.internNext();
			ID result = parseInternally(parts);
			return result;
		}

		default ID parsePrefixed(String dataset, String id) {
			List<String> result = asComponents(dataset, id);
			return parse(result);
		}

		static List<String> asComponents(String dataset, String id) {
			String[] result;
			String[] split = split(id);

			if (dataset == null) {
				return Arrays.asList(split);
			}

			if (split.length > 0 && split[0].equals(dataset)) {
				//if already prefixed
				result = split;
			}
			else {
				result = new String[split.length + 1];
				result[0] = dataset;
				System.arraycopy(split, 0, result, 1, split.length);
			}
			return Arrays.asList(result);
		}
	}

}