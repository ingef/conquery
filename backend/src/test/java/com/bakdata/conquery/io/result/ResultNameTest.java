package com.bakdata.conquery.io.result;

import com.bakdata.conquery.util.io.FileUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultNameTest {

	public static final String FILE_EXTENSION = "test";

	@Test
	public void resultNameOk(){
		final String label = "azAZ19 äü-ÄÜ";
		String fileName = FileUtil.makeSafeFileName(label);
		assertThat(fileName).isEqualTo(label + "." +FILE_EXTENSION);
	}

	@Test
	public void resultNameModified(){

		final String label = "()§ $ \\ \" ";
		String fileName = FileUtil.makeSafeFileName(label);
		assertThat(fileName).isEqualTo("___ _ _ _ ." + FILE_EXTENSION);
	}
}
