package com.bakdata.conquery.models.config;

import java.net.URI;
import java.net.URL;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.fasterxml.jackson.annotation.JsonAlias;
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
	@Valid
	@NotNull
	private CurrencyConfig currency = new CurrencyConfig();

	/**
	 * Years to include in entity preview.
	 */
	@Min(0)
	private int observationPeriodYears = 6;

	@Min(0)
	private int visualisationSamples = 1000;

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


	@Data
	public static class CurrencyConfig {
		@JsonAlias("prefix")
		private String unit = "€";
		private String thousandSeparator = ".";
		private String decimalSeparator = ",";
		private int decimalScale = 2;
	}


}