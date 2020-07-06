package io.github.newlunarfire.openapi.defs.type;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ClassDefinition implements TypeDefinition {
	private Map<String, TypeDefinition> children = new HashMap<String, TypeDefinition>();  
	private String description; 
}
