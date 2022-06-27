package com.bakdata.conquery.apiv1.forms;

import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.internationalization.Localized;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Temporal group indicator  for {@link com.bakdata.conquery.models.forms.util.DateContext}s in {@link com.bakdata.conquery.apiv1.forms.export_form.ExportForm}s. In its result, the indicator is printed for each line, to highlight the belonging.
 * <p>
 * TODO this class can be removed from {@link com.bakdata.conquery.models.forms.util.DateContext} and corresponding functions can be made more general.
 */
@Getter
@RequiredArgsConstructor
public enum FeatureGroup implements Localized {
	FEATURE() {
		@Override
		public String toString(Locale locale) {
			return C10N.get(ResultHeadersC10n.class, locale).featureDateRange();
		}
	},
	OUTCOME() {
		@Override
		public String toString(Locale locale) {
			return C10N.get(ResultHeadersC10n.class, locale).outcomeDateRange();
		}
	},
	SINGLE_GROUP() {
		@Override
		public String toString(Locale locale) {
			return "";
		}
	};

	public static String localizeValue(Object value, PrintSettings cfg) {
		if (value instanceof Resolution) {
			return ((Resolution) value).toString(cfg.getLocale());
		}
		try {
			// If the object was parsed as a simple string, try to convert it to a
			// FeatureGroup to get Internationalization
			return FeatureGroup.valueOf((String) value).toString(cfg.getLocale());
		}
		catch (Exception e) {
			throw new IllegalArgumentException(value + " is not a valid resolution.", e);
		}
	}
}
