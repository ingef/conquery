package com.bakdata.conquery.sql;

import java.io.Closeable;
import java.io.IOException;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

/**
 * Provides access to a configured {@link DSLContext} and enables closing the underlying connection pool.
 */
@RequiredArgsConstructor
public class DSLContextWrapper implements Closeable {

	@Getter
	private final DSLContext dslContext;

	private final HikariDataSource dataSource;

	@Override
	public void close() throws IOException {
		// Hikari opens a connection pool under the hood which we won't be able to close after passing it to the DSLContext.
		// That's why we keep the HikariDataSource reference.
		dataSource.close();
	}

}
