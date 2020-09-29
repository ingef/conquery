package com.bakdata.conquery.models.events.generation;

import java.util.List;

import com.bakdata.conquery.util.ConqueryJavaEscape;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public enum SafeName implements TemplateMethodModelEx {
	
	INSTANCE;
	
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if(arguments.size() != 1 || !(arguments.get(0) instanceof SimpleScalar)) {
			throw new TemplateModelException("safeName requires exactly one string argument");
		}
		String name = ((SimpleScalar) arguments.get(0)).getAsString();
		return new SimpleScalar(ConqueryJavaEscape.escape(name));
	}
}
