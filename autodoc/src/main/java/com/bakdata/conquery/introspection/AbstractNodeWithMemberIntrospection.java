package com.bakdata.conquery.introspection;

import java.io.File;
import java.util.Arrays;

import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.google.common.collect.MoreCollectors;
import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodParameterInfo;
import io.github.classgraph.TypeSignature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AbstractNodeWithMemberIntrospection<VALUE extends NodeWithJavadoc<?> & NodeWithRange<?> & NodeWithMembers<?>> extends AbstractJavadocIntrospection<VALUE> {

	public AbstractNodeWithMemberIntrospection(File file, VALUE nodeWithJavadoc) {
		super(file, nodeWithJavadoc);
	}

	@Override
	public Introspection findMethod(MethodInfo method) {
		var types = Arrays.stream(method.getParameterInfo())
						  .map(MethodParameterInfo::getTypeSignatureOrTypeDescriptor)
						  .map(this::toClass)
						  .toArray(Class[]::new);

		return new AbstractJavadocIntrospection<>(
				file,
				value
						.getMethodsByParameterTypes(types)
						.stream()
						.filter(md -> md.getNameAsString().equals(method.getName()))
						.collect(MoreCollectors.onlyElement())
		);

	}

	@Override
	public Introspection findField(FieldInfo field) {

		var f = value.getFieldByName(field.getName());
		if (f.isPresent()) {
			FieldDeclaration fieldDeclaration = f.get();
			return new AbstractJavadocIntrospection<>(file, fieldDeclaration);
		}
		log.warn("Could not find field '{}'", field.getName());
		return new SimpleIntrospection(file);
	}

	public Introspection findInnerType(String simpleName) {
		for (var decl : value.getMembers()) {
			if (decl instanceof NodeWithSimpleName<?> node) {
				if (!node.getNameAsString().equals(simpleName)) {
					continue;
				}
			}
			else {
				continue;
			}
			if (decl instanceof EnumDeclaration enumDeclaration) {
				return new EnumIntrospection(file, enumDeclaration);
			}
			if (decl instanceof TypeDeclaration<?> cType) {
				return new AbstractNodeWithMemberIntrospection<>(file, cType);
			}
		}
		throw new IllegalStateException(value.getNameAsString() + " has no inner type " + simpleName);
	}

	private Class<?> toClass(TypeSignature sig) {
		if (sig instanceof BaseTypeSignature) {
			return ((BaseTypeSignature) sig).getType();
		}
		else if (sig instanceof ArrayTypeSignature) {
			return ((ArrayTypeSignature) sig).loadClass();
		}
		else if (sig instanceof ClassRefTypeSignature) {
			return ((ClassRefTypeSignature) sig).loadClass();
		}
		throw new IllegalStateException("Can't find class for signature " + sig);
	}

}
