package com.bakdata.conquery.models.config;

import java.net.URI;
import java.net.URL;
import java.util.Currency;

import javax.annotation.Nullable;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Range;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@With
@Data
public class FrontendConfig {
	@NotNull
	private String currencyCode = "EUR";

	@JsonIgnore
	public Currency getCurrency() {
		return Currency.getInstance(currencyCode);
	}

	/**
	 * Years to include in entity preview.
	 */
	@Min(0)
	private int observationPeriodYears = 6;

	/**
	 * Limit to number of histogram entries.
	 * Note, that zero and out of bounds values are tracked in separate bins, so you can have three additional bins.
	 */
	@Min(0)
	private int visualisationsHistogramLimit = 10;

	private Range<Integer> visualisationPercentiles = Range.closed(15, 85);
	/**
	 * The url that points a manual. This is also used by the {@link FormScanner}
	 * as the base url for forms that specify a relative url. Internally {@link URI#resolve(URI)}
	 * is used to concatenate this base url and the manual url from the form.
	 * An final slash ('{@code /}') on the base url has the following effect:
	 * <ul>
	 *     <li>
	 *         <strong>No slash:</strong><br/>
	 *         {@code baseUrl = http://example.org/manual/welcome}<br/>
	 *         {@code formUrl = ./form}<br/>
	 *         &#8594; {@code http://example.org/manual/form}
	 *     </li>
	 *     <li>
	 * 	       <strong>Slash:</strong><br/>
	 *           {@code baseUrl = http://example.org/manual/}<br/>
	 *           {@code formUrl = ./form}<br/>
	 * 	       &#8594; {@code http://example.org/manual/form}
	 * 	   </li>
	 * </ul>
	 */
	@Nullable
	private URL manualUrl;
	@Nullable
	@Email
	private String contactEmail;
	/**
	 * If true, users are always allowed to add custom values into SelectFilter input fields.
	 */
	private boolean alwaysAllowCreateValue = false;

	@ValidationMethod(message = "Percentiles must be concrete and within 0 - 100")
	@JsonIgnore
	public boolean isValidPercentiles() {
		if (!visualisationPercentiles.hasLowerBound() || !visualisationPercentiles.hasUpperBound()) {
			return false;
		}

		if (visualisationPercentiles.lowerEndpoint() < 0) {
			return false;
		}

		if (visualisationPercentiles.upperEndpoint() > 100) {
			return false;
		}

		return true;
	}

	@ValidationMethod(message = "Currency Code unknown.")
	@JsonIgnore
	public boolean isValidCurrencyCode() {
		return Currency.getInstance(getCurrencyCode()) != null;
	}

}