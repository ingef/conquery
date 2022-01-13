package com.bakdata.conquery.apiv1.forms;

import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
}
