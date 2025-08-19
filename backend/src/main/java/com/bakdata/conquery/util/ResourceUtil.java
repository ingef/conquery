package com.bakdata.conquery.util;

import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceUtil {

	public static String wrapAsUriTemplate(String templateString) {
		Preconditions.checkArgument(templateString.matches("^[a-z-_A-Z]+(: .+)?"), "Provided template string does not match allowed format (see https://docs.oracle.com/cd/E19798-01/821-1841/6nmq2cp26/index.html). Was: %s", templateString);
		return "{" + templateString + "}";
	}

}
