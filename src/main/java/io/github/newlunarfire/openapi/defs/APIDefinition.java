package io.github.newlunarfire.openapi.defs;

import java.util.List;

import lombok.Data;

@Data
public class APIDefinition {
	private String title;
	private String version;
	private String servers;
	private List<ResourceDefinition> resources;
}
