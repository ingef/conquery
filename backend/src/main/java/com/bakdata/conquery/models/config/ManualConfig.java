package com.bakdata.conquery.models.config;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.validation.ManualURI;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration of manual links for {@link org.checkerframework.checker.signature.qual.InternalForm} and
 * overriding of other {@link com.bakdata.conquery.apiv1.forms.Form}s.
 */
@CPSType(id = "MANUAL", base = PluginConfig.class)
@Getter
@Setter
public class ManualConfig implements PluginConfig {


	/**
	 * Maps a {@link CPSType} of a {@link com.bakdata.conquery.apiv1.forms.Form} to a manual URL.
	 * E.g.:
	 * <ul>
	 * 		<li>EXPORT_FORM -> https://example.org/export-form</li>
	 * 		<li>FULL_EXPORT_FORM -> ./full-export-form</li>
	 * </ul>
	 */
	private Map<String, @ManualURI URI> forms = Collections.emptyMap();

}
