package io.github.newlunarfire.openapi.defs.type;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class EnumDefinition implements TypeDefinition {
	private String description;
	private Map<String, String> values = new HashMap<String, String>();
}
