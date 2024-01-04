package com.bakdata.conquery.integration.json;

import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.config.Dialect;
import lombok.Value;

@Value
public class SqlSpec {

	boolean isEnabled;

	@Nullable
	List<Dialect> supportedDialects;

	/**
	 * @return True if a test spec is allowed for a specific dialect.
	 */
	public boolean isAllowedTest(Dialect dialect) {
		if (dialect == null) {
			return true;
		}
		return supportedDialects == null || supportedDialects.contains(dialect);
	}

}
