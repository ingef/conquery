package com.bakdata.conquery.apiv1.forms;

import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.internationalization.Localized;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

	@CPSType(id = "OBSERVATION_SCOPE", base = Localized.Provider.class)
	public static class LocalizationProvider implements Localized.Provider {

		@Override
		public String apply(Object o, Locale locale) {
			return FeatureGroup.valueOf((String)o).toString(locale);
		}
	}
}
