package com.bakdata.eva.models.translation.query.oldmodel.form;

import java.net.URL;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FormAnswer {
	@NotNull
	private URL url;
	@Min(1)
	private long fileSize;
	@NotEmpty
	private String fileName;
	@NotEmpty
	private String mimeType;
}
