package io.github.newlunarfire.openapi.defs;

import java.util.HashMap;
import java.util.Map;

import io.github.newlunarfire.openapi.defs.type.TypeDefinition;
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
	private Map<String, TypeDefinition> pathParameters = new HashMap<String, TypeDefinition>();
	private Map<String, String> pathParameterDescriptions = new HashMap<String, String>();
	private TypeDefinition returnType;
	private TypeDefinition requestBody;
	
	public void setProduces(String produces) {
		this.produces = produces.replace("\"", "");
	}
	
	public void setConsumes(String consumes) {
		this.consumes = consumes.replace("\"", "");
	}
	
	public void addPathParameter(String name, TypeDefinition type) {
		pathParameters.put(name, type);
	}
	
	public String getParameterDescription(String name) {
		return pathParameterDescriptions.get(name);
	}
}