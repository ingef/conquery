package com.bakdata.conquery.util;

import javax.ws.rs.NotFoundException;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceUtil {

	public static void throwNotFoundIfNull(@NonNull Id<?> id, Object identifiable) {
		if (identifiable == null) {
			throw new NotFoundException(id.toString());
		}
	}

	public static String wrapAsUriTemplate(String templateString) {
		Preconditions.checkArgument(templateString.matches("^[a-z-_A-Z]+(: .+)?"), "Provided template string does not match allowed format (see https://docs.oracle.com/cd/E19798-01/821-1841/6nmq2cp26/index.html). Was: %s", templateString);
		return "{" + templateString + "}";
	}

}
