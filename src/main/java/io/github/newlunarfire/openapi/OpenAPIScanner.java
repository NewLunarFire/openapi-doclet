package io.github.newlunarfire.openapi;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor9;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;

import io.github.newlunarfire.openapi.defs.APIDefinition;
import io.github.newlunarfire.openapi.defs.MethodDefinition;
import io.github.newlunarfire.openapi.defs.ResourceDefinition;
import io.github.newlunarfire.openapi.defs.type.TypeDefinition;
import io.github.newlunarfire.openapi.defs.type.ClassDefinition;
import io.github.newlunarfire.openapi.defs.type.EnumDefinition;
import io.github.newlunarfire.openapi.defs.type.ListDefinition;
import io.github.newlunarfire.openapi.defs.type.PrimitiveDefinition;
import jdk.javadoc.doclet.DocletEnvironment;

import lombok.Getter;
import lombok.Setter;

public class OpenAPIScanner {
	@Getter
	@Setter
	private String title;
	
	@Getter
	@Setter
	private String version;
	
	@Getter
	@Setter
	private String servers;
	
	@Getter
	@Setter
	private List<String> packages;
	
	private HashMap<String, TypeElement> typeElements = new HashMap<String, TypeElement>();
	private DocTrees docTrees; 
	
	public APIDefinition scan(DocletEnvironment environment) {
		Elements eUtils = environment.getElementUtils();
		this.docTrees = environment.getDocTrees();
		
		for(var t : ElementFilter.typesIn(environment.getIncludedElements())) {
			typeElements.put(t.getSimpleName().toString(), t);
		}
		
		APIDefinition api = new APIDefinition();
		api.setTitle(this.title);
		api.setVersion(this.version);
		api.setServers(this.servers);
		
		api.setResources(
			ElementFilter.typesIn(environment.getIncludedElements())
				.stream()
				.filter(t -> this.packages.contains(eUtils.getPackageOf(t).getQualifiedName().toString()))
				.map(this::scanResource)
				.collect(Collectors.toList())
		);
		
		return api;
	}
	
	public ResourceDefinition scanResource(TypeElement t) {
		ResourceDefinition resource = new ResourceDefinition();
		
		HashMap<String, String> annotationValues = mapWithValues(t.getAnnotationMirrors());
		resource.setBasePath(annotationValues.get(Constants.OPENAPIDOCLET_PATH_ANNOTATION));

		resource.setMethods(
			t.getEnclosedElements().stream()
				.filter(e -> (e instanceof ExecutableElement))
				.map(e -> scanMethod((ExecutableElement) e))
				.filter(e -> e.isPresent())
				.map(e -> e.get())
				.collect(Collectors.toList())
		);
		
		return resource;
	}
	
	public DocCommentTree getDocTree(Element e) {
		return docTrees.getDocCommentTree(e);
	}
	
	private HashMap<String, String> mapWithValues(List<? extends AnnotationMirror> list) {
		HashMap<String, String> annotationValues = new HashMap<String, String>();

		for(AnnotationMirror annotation : list) {
			annotationValues.put(annotation.getAnnotationType().toString(), getAnnotationValue(annotation));
		}

		return annotationValues;
	}

	private String getAnnotationValue(AnnotationMirror annotation) {
		return annotation.getElementValues().keySet().stream()
				.filter(ex -> ex.getSimpleName().toString().equals("value"))
				.map(ex -> (String) annotation.getElementValues().get(ex).getValue().toString())
				.findFirst()
				.orElse(null);
	}
	
	private Optional<MethodDefinition> scanMethod(ExecutableElement e) {
		final MethodDefinition definition = new MethodDefinition();
		final DocCommentTree docCommentTree = docTrees.getDocCommentTree(e);
		
		HashMap<String, String> annotationValues = mapWithValues(e.getAnnotationMirrors());
		
		for(String methodVerb: Constants.OPENAPIDOCLET_METHOD_VERBS) {
			if(annotationValues.containsKey(Constants.OPENAPIDOCLET_JAVAWSRS_PACKAGE + "." + methodVerb)) {
				definition.setVerb(methodVerb);
			}
		}
		
		// Needs to have at least one "verb" annotation
		if(definition.getVerb() == null) {
			return Optional.empty();
		}
		
		// Scan parameters
		for(var param : e.getParameters()) {
			scanParameter(definition, param);
		}
		
		if(annotationValues.containsKey(Constants.OPENAPIDOCLET_PATH_ANNOTATION)) {
			definition.setPath(annotationValues.get(Constants.OPENAPIDOCLET_PATH_ANNOTATION));
		}
		
		if(annotationValues.containsKey(Constants.OPENAPIDOCLET_PRODUCES_ANNOTATION)) {
			definition.setProduces(annotationValues.get(Constants.OPENAPIDOCLET_PRODUCES_ANNOTATION));
		}
		
		if(annotationValues.containsKey(Constants.OPENAPIDOCLET_CONSUMES_ANNOTATION)) {
			definition.setConsumes(annotationValues.get(Constants.OPENAPIDOCLET_CONSUMES_ANNOTATION));
		}
		
		if (docCommentTree != null) {
			definition.setBody(docCommentTree.getFullBody().toString());
			definition.setBlockTags(docCommentTree.getBlockTags().toString());
			
			for(var block : docCommentTree.getBlockTags()) {
				block.accept(new SimpleDocTreeVisitor<Void, OpenAPIScanner>() {
					@Override
					public Void visitUnknownBlockTag(UnknownBlockTagTree node, OpenAPIScanner p) {
						if("returnType".equals(node.getTagName())) {
							String type = node.getContent().toString();
							TypeDefinition td = null;
							
							if(type.endsWith(".class")) {
								TypeElement te = typeElements.get(type.replace(".class", ""));
								
								if(te != null) {
									td = scanType(te);
								}
							}
							
							definition.setReturnType(td);
						}
						
						return null;
					}
					
					@Override
					public Void visitParam(ParamTree node, OpenAPIScanner p) {
						definition.getPathParameterDescriptions().put(node.getName().toString(), node.getDescription().toString());
				        return null;
				    }
				}, this);
			}
		}
		
		return Optional.of(definition);
	}
	
	private TypeDefinition scanType(Element e) {
		final DocCommentTree docCommentTree = docTrees.getDocCommentTree(e);
		final TypeDefinition td = scanType(e.asType());
		
		if(docCommentTree != null) {
			td.setDescription(docCommentTree.getFullBody().toString());
		}
		
		return td;
	}
	
	private TypeDefinition scanType(TypeMirror type) {
		if("java.lang.String".equals(type.toString())) {
			final PrimitiveDefinition pDef = new PrimitiveDefinition();
			pDef.setType("string");
			return pDef;
		}
		
		return type.accept(new SimpleTypeVisitor9<TypeDefinition, Void>()  {
			@Override
			public TypeDefinition visitDeclared(DeclaredType t, Void p) {
				if(type.toString().startsWith("java.util.List")) {
					return visitList(t, p);
				}
				
				final Element e = t.asElement();
				final boolean isEnum = e.getEnclosedElements().stream()
						.filter(el -> el.getKind() == ElementKind.ENUM_CONSTANT)
						.findAny()
						.isPresent();
				
				if(isEnum) {
					return visitEnum(t, p);
				}
				
				final ClassDefinition classDef = new ClassDefinition();
				
				for(var el : e.getEnclosedElements()) {
					if(el.getKind() == ElementKind.FIELD) {
						classDef.getChildren().put(el.getSimpleName().toString(), scanType(el));
					}
				}
				
		 		return classDef;
		 	}
		 	
			@Override
			public TypeDefinition visitPrimitive(PrimitiveType t, Void p) {
				final PrimitiveDefinition pDef = new PrimitiveDefinition();
				pDef.setType(t.toString());
				
				return pDef;
		 	}
			
			public TypeDefinition visitList(DeclaredType t, Void p) {
				final ListDefinition lDef = new ListDefinition();
				lDef.setSubType(scanType(t.getTypeArguments().get(0)));
				return lDef;
			}
			
			public TypeDefinition visitEnum(DeclaredType t, Void p) {
				final EnumDefinition eDef = new EnumDefinition();
				final List<Element> constants = t.asElement().getEnclosedElements().stream()
					.filter(el -> el.getKind() == ElementKind.ENUM_CONSTANT)
					.collect(Collectors.toList());
				
				for(Element constant: constants) {
					String comment = Optional.ofNullable(docTrees.getDocCommentTree(constant))
											.map(docTree -> docTree.getFullBody().toString())
											.orElse(null);
					
					eDef.getValues().put(constant.toString(), comment);
				}
				
				return eDef;
			}
		}, null);
	}
	
	private void scanParameter(MethodDefinition definition, VariableElement e) {
		HashMap<String, String> annotations = mapWithValues(e.getAnnotationMirrors());
		final boolean isPathParam = annotations.containsKey(Constants.OPENAPIDOCLET_PATHPARAM_ANNOTATION);
		
		TypeDefinition td = scanType(e);
		
		if(isPathParam) {
			definition.addPathParameter(e.getSimpleName().toString(), td);
		} else {
			definition.setRequestBody(td);
		}
	}
}
