package com.bakdata.conquery.models.config;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

/**
 * This validator checks a common use case for the column name creation. It may be extended.
 * For now the parsing, compilation and a column name creation based on the labels is checked.
 *
 */
@Slf4j
public class ColumnNamerValidator implements ConstraintValidator<ValidColumnNamer, String> {
	private static final String SELECT_NAME = "selectName";
	private static final String CONCEPT_NAME = "conceptName";
	
	private static final CQConcept CQ_CONCEPT = new CQConcept();
	private static final Select SELECT = new Select() {
		@Override
		public Aggregator<?> createAggregator() {
			return null;
		}
	};
	private static final VirtualConcept V_CONCEPT = new VirtualConcept();
	
	static {
		CQ_CONCEPT.setLabel(CONCEPT_NAME);
		SELECT.setName(SELECT_NAME);
		SELECT.setHolder(V_CONCEPT);
	}
	
	

	@Override
	public void initialize(ValidColumnNamer constraintAnnotation) {
		// Nothing to initialize
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		String scriptString = value;
		
		if(Strings.isNullOrEmpty(scriptString)) {
			context.buildConstraintViolationWithTemplate(String.format("Column Namer Script is not allowed to be null or empty")).addConstraintViolation();
			return false;
		}
		
		PrintSettings setting = new PrintSettings(true, value);
		
		/*
		 * Instantiate a column info. Be aware that this instance is not fully resolved (e.g. json backreferences are not set),
		 * so the validator might fail if the script intents to use these.
		 * Regarding this aspect, the validator has to be extend.
		 */
		SelectResultInfo columnInfo = new SelectResultInfo(setting,SELECT, CQ_CONCEPT);
		

		// Check if script can be evaluated
		try {
			columnInfo.getUniqueName();
		}
		catch (Exception e) {
			context.buildConstraintViolationWithTemplate(String.format("Column Namer Script could not be parsed/compiled: %s", e)).addConstraintViolation();
			return false;
		}
		
		
		log.info(String.format("Configured column namer script is okay: %s", scriptString));
		
		return true;
	}

}
