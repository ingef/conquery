package com.bakdata.conquery.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import javax.annotation.Nullable;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

@UtilityClass
public class LogUtil {

	/**
	 * Helper method to avoid compiler warnings of either:
	 * <ul>
	 *     <li><code>log.warn("Unable to get index: {} (enable TRACE for exception)", key, log.isTraceEnabled() ? e : null);</code> -> More arguments provided (2) than placeholders specified (1)</li>
	 *     <li><code>log.warn("Unable to get index: {} (enable TRACE for exception)", key, (Exception) (log.isTraceEnabled() ? e : null));</code> -> Redundant cast</li>
	 * </ul>
	 * @param log The logger to test the level on
	 * @param e The exception to pass if logger threshold is satisfied
	 * @return The exception if the trace level is enabled otherwise <code>null</code>
	 */
	@Nullable
	public Throwable passExceptionOnTrace(Logger log, Throwable e) {
		return log.isTraceEnabled() ? e : null;
	}

	public String printPath(File f) {
		return printPath(f.toPath());
	}
	
	public String printPath(Path p) {
		try {
			return p.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
		} catch (IOException e) {
			return p.toAbsolutePath().toString();
		}
	}

}
