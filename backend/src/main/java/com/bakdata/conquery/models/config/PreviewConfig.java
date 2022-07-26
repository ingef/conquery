package com.bakdata.conquery.models.config;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PreviewConfig {
	private List<SelectId> infoCardSelects = List.of();

}
