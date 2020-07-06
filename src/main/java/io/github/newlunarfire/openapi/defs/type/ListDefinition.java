package io.github.newlunarfire.openapi.defs.type;

import lombok.Data;

@Data
public class ListDefinition implements TypeDefinition {
	private String description;
	private TypeDefinition subType;
}
