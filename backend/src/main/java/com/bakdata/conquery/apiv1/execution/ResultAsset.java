package com.bakdata.conquery.apiv1.execution;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public record ResultAsset(String label, URI url) {

	@JsonIgnore
	@ValidationMethod(message = "Generated assetId was blank")
	public boolean isAssetIdValid() {
		return StringUtils.isBlank(getAssetId());
	}

	/**
	 * An url-encoded id of this asset.
	 * It derives from the last path section of the file part in this url.
	 */
	@JsonIgnore
	public String getAssetId() {

		final String path = url.getPath();
		return URLEncoder.encode(path.substring(path.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
	}
}
