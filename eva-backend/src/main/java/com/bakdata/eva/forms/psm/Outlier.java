package com.bakdata.eva.forms.psm;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class Outlier {
	
	public static enum OutlierMode {NO, ALIVE};
	
	@NotNull
	private OutlierMode mode;
	private Integer threshold;
}