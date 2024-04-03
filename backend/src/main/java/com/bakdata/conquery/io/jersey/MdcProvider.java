package com.bakdata.conquery.io.jersey;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@RequiredArgsConstructor
public class MdcProvider implements Feature {
	@NonNull
	private final String node;

	@Override
	public boolean configure(FeatureContext context) {

		context.register(new AbstractBinder() {

			@Override
			protected void configure() {
				bind(node).named(ConqueryMDC.NODE).to(String.class);
			}
		});
		context.register(MdcNodeFilter.class);
		return true;
	}
}
