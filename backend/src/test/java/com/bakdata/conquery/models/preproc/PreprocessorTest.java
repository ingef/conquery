package com.bakdata.conquery.models.preproc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

class PreprocessorTest {

	@Test
	void getTaggedVersion() {
		assertThat(Preprocessor.getTaggedVersion(new File("file"), "tag", ".csv")).hasName("file");
		assertThat(Preprocessor.getTaggedVersion(new File("file.csv"), "tag", ".csv")).hasName("file.tag.csv");
		assertThat(Preprocessor.getTaggedVersion(new File("file.csv.csv"), "tag", ".csv")).hasName("file.csv.tag.csv");
	}
}