package com.bakdata.conquery.models.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.query.resultinfo.ExternalResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.types.ResultType;
import org.junit.jupiter.api.Test;

public class UniqueNameTest {
	@Test
	void testNameCollision() {
		PrintSettings settings = new PrintSettings(true, Locale.ROOT, null, new ConqueryConfig(), null);
		final UniqueNamer uniqueNamer = new UniqueNamer(settings);

		final ExternalResultInfo info1 = new ExternalResultInfo("test", ResultType.Primitive.STRING, settings);
		final ExternalResultInfo info2 = new ExternalResultInfo("test", ResultType.Primitive.STRING, settings);
		final ExternalResultInfo info3 = new ExternalResultInfo("test_1", ResultType.Primitive.STRING, settings);
		final ExternalResultInfo info4 = new ExternalResultInfo("test", ResultType.Primitive.STRING, settings);

		assertThat(uniqueNamer.getUniqueName(info1)).isEqualTo("test");
		assertThat(uniqueNamer.getUniqueName(info2)).isEqualTo("test_1");
		assertThat(uniqueNamer.getUniqueName(info3)).isEqualTo("test_1_1");
		assertThat(uniqueNamer.getUniqueName(info4)).isEqualTo("test_2");
	}
}
