package com.bakdata.conquery.models.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import org.junit.jupiter.api.Test;

public class UniqueNameTest {
	@Test
	void testNameCollision() {
		final UniqueNamer uniqueNamer = new UniqueNamer(new PrintSettings(true, Locale.ROOT, null, new ConqueryConfig(), null));

		final SimpleResultInfo info1 = new SimpleResultInfo("test", null);
		final SimpleResultInfo info2 = new SimpleResultInfo("test", null);
		final SimpleResultInfo info3 = new SimpleResultInfo("test_1", null);
		final SimpleResultInfo info4 = new SimpleResultInfo("test", null);

		assertThat(uniqueNamer.getUniqueName(info1)).isEqualTo("test");
		assertThat(uniqueNamer.getUniqueName(info2)).isEqualTo("test_1");
		assertThat(uniqueNamer.getUniqueName(info3)).isEqualTo("test_1_1");
		assertThat(uniqueNamer.getUniqueName(info4)).isEqualTo("test_2");
	}
}
