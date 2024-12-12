package com.bakdata.conquery.io.jackson.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;

/**
 * MixIn to suppress artificial properties of {@link DefaultSocketSessionConfig}.
 */
@JsonIgnoreProperties(value = {
		"throughputCalculationIntervalInMillis",
		"readerIdleTimeInMillis",
		"writeTimeoutInMillis",
		"writerIdleTimeInMillis",
		"bothIdleTimeInMillis"
})
public class DefaultSocketSessionConfigMixIn extends DefaultSocketSessionConfig {
}
