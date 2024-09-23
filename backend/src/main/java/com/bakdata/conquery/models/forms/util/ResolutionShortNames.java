package com.bakdata.conquery.models.forms.util;

import java.util.stream.Stream;

/**
 * This enum exists for compatibility reasons with the stratification
 * dropdown menu in the frontend. This way the actual Resolution/Alignment logic does not need to be touched.
 * <br><br>
 * Because of uniformity and accessibility there are options like
 * {@link ResolutionShortNames#COMPLETE_YEARS_QUARTERS}, for which the frontend could send a list of resolutions (<code>"value": ["COMPLETE","YEARS", "QUARTERS"]</code>).
 * <br>
 * However, setting this list as the <strong>default option</strong> does not work (see export_form.frontend_conf.json).
 * Only a string applies, hence <code>"value": "COMPLETE_YEARS_QUARTERS"</code>.
 */
public enum ResolutionShortNames {

	// SINGLE RESOLUTIONS (map directly to Resolution)
	COMPLETE {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.COMPLETE);
		}
	},
	YEARS {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.YEARS);
		}
	},
	QUARTERS {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.QUARTERS);
		}
	},
	DAYS {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.DAYS);
		}
	},
	// SPECIAL RESOLUTION SETS
	YEARS_QUARTERS {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.YEARS, Resolution.QUARTERS);
		}
	},
	COMPLETE_YEARS {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.COMPLETE, Resolution.YEARS);
		}
	},
	COMPLETE_QUARTERS {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.COMPLETE, Resolution.QUARTERS);
		}
	},
	COMPLETE_YEARS_QUARTERS {
		@Override
		public Stream<Resolution> correspondingResolutions() {
			return Stream.of(Resolution.COMPLETE, Resolution.YEARS, Resolution.QUARTERS);
		}
	};

	public abstract Stream<Resolution> correspondingResolutions();
}
