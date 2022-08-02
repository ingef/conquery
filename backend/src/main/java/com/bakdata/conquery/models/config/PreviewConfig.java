package com.bakdata.conquery.models.config;

import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PreviewConfig {
	private List<String> infoCardSelects = List.of();

}
