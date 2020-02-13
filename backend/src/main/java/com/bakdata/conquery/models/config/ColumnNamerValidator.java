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
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

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
	
	private final GroovyShell groovyShell = new GroovyShell(new CompilerConfiguration());
	

	@Override
	public void initialize(ValidColumnNamer constraintAnnotation) {
		// Nothing to initialize
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {		
				String scriptString = value;
		
		if(Strings.isNullOrEmpty(scriptString)) {
			// No script provided defaults to PrintSettings#standardColumnName
			return true;
		}
		
		context.disableDefaultConstraintViolation();
		
		/*
		 * Instantiate a column info. Be aware that this instance is not fully resolved (e.g. json backreferences are not set),
		 * so the validator might fail if the script intents to use these.
		 * Regarding this aspect, the validator has to be extend.
		 */
		SelectResultInfo columnInfo = new SelectResultInfo(SELECT, CQ_CONCEPT);
		
		groovyShell.setProperty(PrintSettings.GROOVY_VARIABLE, columnInfo);

		// Check if script can be parsed
		Script script;
		try {
			script  = groovyShell.parse(scriptString);
		}
		catch (CompilationFailedException|IllegalArgumentException e) {
			context.buildConstraintViolationWithTemplate(String.format("Column Namer Script could not be parsed/compiled: %s", e)).addConstraintViolation();
			return false;
		}
		
		
		// Check if script handles a simple SelectResultInfo
		try {
			script.run();
		}
		catch (Exception e) {
			context.buildConstraintViolationWithTemplate(String.format("Column Namer Script failed execution: %s",e)).addConstraintViolation();
			return false;
		}
		
		
		log.info(String.format("Configured column namer script is okay: %s", scriptString));
		
		return true;
	}

}
