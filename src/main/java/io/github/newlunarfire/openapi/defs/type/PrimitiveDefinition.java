package io.github.newlunarfire.openapi.defs.type;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PrimitiveDefinition implements TypeDefinition {
	private String description;
	private String type;
}
