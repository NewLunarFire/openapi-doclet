package io.github.newlunarfire.openapi.defs;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

import lombok.Data;

@Data
public class MethodDefinition {
	private String name;
	private String consumes;
	private String produces;
	private String verb;
	private String path;
	private String body;
	private String blockTags;
	private Map<String, TypeMirror> pathParameters = new HashMap<String, TypeMirror>();
	private Map<String, String> pathParameterDescriptions = new HashMap<String, String>();
	private Class<?> returnType;
	private TypeMirror requestBody;
	
	public void setProduces(String produces) {
		this.produces = produces.replace("\"", "");
	}
	
	public void setConsumes(String consumes) {
		this.consumes = consumes.replace("\"", "");
	}
	
	public void addPathParameter(String name, TypeMirror type) {
		pathParameters.put(name, type);
	}
	
	public String getParameterDescription(String name) {
		return pathParameterDescriptions.get(name);
	}
}