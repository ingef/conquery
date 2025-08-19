package com.bakdata.conquery.introspection;

import java.io.File;

import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AbstractJavadocIntrospection<VALUE extends NodeWithJavadoc<?> & NodeWithRange<?>> implements Introspection {

	@Getter
	protected final File file;
	@Getter
	protected final VALUE value;

	@Getter(lazy = true)
	private final String description = extractDescription();
	@Getter(lazy = true)
	private final String example = extractExample();

	protected String extractDescription() {
		if (value.getJavadoc().isEmpty()) {
			return "";
		}
		var javadoc = value.getJavadoc().get();
		return javadoc.getDescription().toText().replaceAll("\\s+", " ");
	}

	private String extractExample() {
		if (value.getJavadoc().isEmpty()) {
			return "";
		}
		var javadoc = value.getJavadoc().get();
		return javadoc.getBlockTags()
					  .stream()
					  .filter(b -> b.getTagName().equals("jsonExample"))
					  .findAny()
					  .map(JavadocBlockTag::getContent)
					  .map(JavadocDescription::toText)
					  .orElse("");
	}

	@Override
	public String getLine() {
		if (value.getJavadocComment().isPresent()) {
			return "L"
				   + value.getJavadocComment().get().getBegin().get().line
				   + "-L"
				   + value.getJavadocComment().get().getEnd().get().line;
		}
		return "L" + value.getBegin().get().line;
	}

	@Override
	public Introspection findMethod(MethodInfo method) {
		return new SimpleIntrospection(file);
	}

	@Override
	public Introspection findField(FieldInfo field) {
		return new SimpleIntrospection(file);
	}
}
