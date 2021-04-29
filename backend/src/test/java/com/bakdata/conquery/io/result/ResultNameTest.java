package com.bakdata.conquery.io.result;

import com.bakdata.conquery.util.io.FileUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultNameTest {

	@Test
	public void resultNameOk(){
		final String label = "azAZ19 äü-ÄÜ";
		String fileName = FileUtil.makeSafeFileName("test", label);
		assertThat(fileName).isEqualTo(label + ".test");
	}

	@Test
	public void resultNameModified(){

		final String label = "()§ $ \\ \" ";
		String fileName = FileUtil.makeSafeFileName("test", label);
		assertThat(fileName).isEqualTo("___ _ _ _ " + ".test");
	}
}
