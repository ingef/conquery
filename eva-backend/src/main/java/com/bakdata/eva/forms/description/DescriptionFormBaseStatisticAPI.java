package com.bakdata.eva.forms.description;

import javax.validation.constraints.NotNull;

import com.bakdata.eva.models.forms.DateContextMode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescriptionFormBaseStatisticAPI {
	
	@NotNull
	protected DateContextMode resolution;
}
