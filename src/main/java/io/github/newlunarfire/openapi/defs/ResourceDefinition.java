package io.github.newlunarfire.openapi.defs;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ResourceDefinition {
	private String basePath;
	private List<MethodDefinition> methods = new ArrayList<MethodDefinition>();
}