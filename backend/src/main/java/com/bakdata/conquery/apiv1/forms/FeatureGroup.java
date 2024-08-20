package com.bakdata.conquery.apiv1.forms;

import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Temporal group indicator  for {@link com.bakdata.conquery.models.forms.util.DateContext}s in {@link com.bakdata.conquery.apiv1.forms.export_form.ExportForm}s. In its result, the indicator is printed for each line, to highlight the belonging.
 * <p>
 * TODO this class can be removed from {@link com.bakdata.conquery.models.forms.util.DateContext} and corresponding functions can be made more general.
 */
@Getter
@RequiredArgsConstructor
public enum FeatureGroup {
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

	public abstract String toString(Locale locale);

	@RequiredArgsConstructor
	public static class LocalizingPrinter implements ResultPrinters.Printer {
		private final PrintSettings cfg;

		@Override
		public String print(Object f) {
			if (f instanceof Resolution) {
				return ((Resolution) f).toString(cfg.getLocale());
			}
			try {
				// If the object was parsed as a simple string, try to convert it to a
				// FeatureGroup to get Internationalization
				return FeatureGroup.valueOf((String) f).toString(cfg.getLocale());
			}
			catch (Exception e) {
				throw new IllegalArgumentException(f + " is not a valid resolution.", e);
			}
		}
	}

}
