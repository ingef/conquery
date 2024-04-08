package com.bakdata.conquery.io.external.form;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class FormBackendVersion {
	private String version;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date buildTime;
}
