package com.bakdata.conquery.io.mina;

import com.bakdata.conquery.util.io.ConqueryMDC;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.filterchain.IoFilterEvent;
import org.apache.mina.filter.util.CommonEventFilter;

@RequiredArgsConstructor
public class ConqueryMdcFilter extends CommonEventFilter {
	final private String node;

	private final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

	/**
	 * Adapted from {@link org.apache.mina.filter.logging.MdcInjectionFilter}
	 */
	@Override
	protected void filter(IoFilterEvent event) throws Exception {

		// since this method can potentially call into itself
		// we need to check the call depth before clearing the MDC
		int currentCallDepth = callDepth.get();
		callDepth.set(currentCallDepth + 1);

		if (currentCallDepth == 0) {
			/* copy context to the MDC when necessary. */
			ConqueryMDC.NODE.set(node);
			ConqueryMDC.LOCATION.set(event.getSession().getLocalAddress().toString());
		}

		try {
			/* propagate event down the filter chain */
			event.fire();
		}
		finally {
			if (currentCallDepth == 0) {
				/* remove context from the MDC */
				ConqueryMDC.NODE.clear();
				ConqueryMDC.LOCATION.clear();

				callDepth.remove();
			}
			else {
				callDepth.set(currentCallDepth);
			}
		}


	}
}
