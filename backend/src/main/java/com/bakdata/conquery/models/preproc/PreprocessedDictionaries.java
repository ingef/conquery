package com.bakdata.conquery.models.preproc;

import java.util.Map;

import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class PreprocessedDictionaries {
	@NotNull
	private final Dictionary primaryDictionary;
	@CheckForNull
	private final Map<String, Dictionary> dictionaries;
}
