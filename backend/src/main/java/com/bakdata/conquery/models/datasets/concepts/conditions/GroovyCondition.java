package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * A condition that is a groovy script and thus able to represent everything.
 */
@Slf4j @CPSType(id="GROOVY", base=CTCondition.class)
public class GroovyCondition implements CTCondition {

	public static final String[] AUTO_IMPORTS = Stream.of(
		LocalDate.class,
		Range.class
	).map(Class::getName).toArray(String[]::new);

	@Getter @Setter @NotEmpty
	private String script;
	@JsonIgnore
	private transient ConditionScript compiled;
	@JsonIgnore
	private ConceptTreeNode node;
	
	@Override
	public void init(ConceptTreeNode node) throws ConceptConfigurationException {
		this.node = node;
		compile();
	}
	
	private void compile() throws ConceptConfigurationException {
		if(compiled==null) {
			try {
				CompilerConfiguration config = new CompilerConfiguration();
				config.addCompilationCustomizers(new ImportCustomizer().addImports(AUTO_IMPORTS));
				config.setScriptBaseClass(ConditionScript.class.getName());
				GroovyShell groovy = new GroovyShell(config);
				
				compiled = (ConditionScript) groovy.parse(script);
				compiled.setNode(node);
			} catch(Exception|Error e) {
				throw new ConceptConfigurationException(node,"Failed to compile condition '"+script+"'",e);
			}
		}
	}

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		try {
			return compiled.matches(rowMap.getValue(), value);
		} catch(Exception e) {
			throw new ConceptConfigurationException(node, "Could not execute condition \""+script+"\" on "+rowMap.getValue(), e);
		}
	}


	public abstract static class ConditionScript extends Script {
		
		@Getter
		protected Map<String, Object> row;
		@Getter
		protected String value;
		@Setter
		protected ConceptTreeNode node;
		
		public boolean matches(Map<String, Object> row, String value) throws ConceptConfigurationException {
			this.row=row;
			this.value=value;
			Boolean result = run();
			if(result==null) {
				throw new ConceptConfigurationException(node, "The condition returned null for input '"+value+"'");
			}
			return result;
		}
		
		@Override
		public abstract Boolean run();
		
		@Override
		public Object getProperty(String property) {
			switch(property) {
				case "row": return row;
				case "value": return value;
				default: {
					log.error("\t\t\tAccessed untypical propert '{}' in condition", property);
					return super.getProperty(property);
				}
			}
		}
	}
}
