package com.bakdata.conquery.apiv1.query.concept.specific.external;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@Data
@Slf4j
public abstract class FormatColumn {

	@InternalOnly @Setter @Getter
	private int position;

	static {
		// For each implementation of FormatColumn, test if we can instantiate it using default constructor. Fail start-up if not possible.
		final Set<Class<? extends FormatColumn>> implementations = CPSTypeIdResolver.listImplementations(FormatColumn.class);

		for (Class<? extends FormatColumn> impl : implementations) {
			final String id = impl.getAnnotation(CPSType.class).id();
			try {
				// Try and get no-args constructor
				impl.getConstructor().newInstance();
			}
			catch (Exception e) {
				log.error("FormatColumn {} has no default constructor.", id);
				throw new RuntimeException(e);
			}
		}
	}

}
