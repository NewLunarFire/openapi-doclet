package io.github.newlunarfire.openapi.defs;

import java.util.ArrayList;

import javax.lang.model.element.TypeElement;

import lombok.Data;

@Data
public class TypeDefinition {
	private TypeElement element;
	private String description;
	private ArrayList<TypeDefinition> children = new ArrayList<TypeDefinition>(); 
}
