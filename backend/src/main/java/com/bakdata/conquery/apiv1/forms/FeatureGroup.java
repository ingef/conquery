package com.bakdata.conquery.apiv1.forms;

import java.util.Locale;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.models.common.LocalizedToString;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Temporal group indicator  for {@link com.bakdata.conquery.models.forms.util.DateContext}s in {@link com.bakdata.conquery.apiv1.forms.export_form.ExportForm}s. In its result, the indicator is printed for each line, to highlight the belonging.
 * <p>
 * TODO this class can be removed from {@link com.bakdata.conquery.models.forms.util.DateContext} and corresponding functions can be made more general.
 */
@Getter
@RequiredArgsConstructor
public enum FeatureGroup implements LocalizedToString {
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
	}
}
