package io.github.newlunarfire.openapi.output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import io.github.newlunarfire.openapi.defs.APIDefinition;
import io.github.newlunarfire.openapi.defs.MethodDefinition;
import io.github.newlunarfire.openapi.defs.ResourceDefinition;
import io.github.newlunarfire.openapi.defs.type.ClassDefinition;
import io.github.newlunarfire.openapi.defs.type.EnumDefinition;
import io.github.newlunarfire.openapi.defs.type.ListDefinition;
import io.github.newlunarfire.openapi.defs.type.PrimitiveDefinition;
import io.github.newlunarfire.openapi.defs.type.TypeDefinition;

public class JsonOutputFormatter {
	private static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	private APIDefinition api;
	
	public JsonOutputFormatter(APIDefinition api) {
		this.api = api;
	}
	
	public void output(OutputStream out) throws IOException {
		JsonObject root = new JsonObject();
		JsonObject paths = new JsonObject();
		
		root.addProperty("openapi", "3.0.0");
		root.add("info", createInfo(api));
		root.add("servers", createServers(api.getServers()));
		
		for(ResourceDefinition rdef : api.getResources()) {
			for(MethodDefinition mdef: rdef.getMethods()) {
				String path = rdef.getBasePath() + Optional.ofNullable(mdef.getPath()).orElse("");
				
				if(!paths.has(path)) {
					paths.add(path, new JsonObject());
				}
				
				JsonObject pathObj = paths.get(path).getAsJsonObject();
				JsonObject pathVerbObj = new JsonObject();
				
				if(mdef.getBody() != null) {
					pathVerbObj.addProperty("description", mdef.getBody());
				}
				
				pathVerbObj.add("parameters", getParameters(mdef));
				
				if("POST".equals(mdef.getVerb())) {
					pathVerbObj.add("requestBody", getRequestBody(mdef));
				}
				pathVerbObj.add("responses", getResponses(mdef));
				
				pathObj.add(mdef.getVerb().toLowerCase(), pathVerbObj);
			}
		}
		
		root.add("paths", paths);
		
		out.write(gson.toJson(root).getBytes());
	}
	
	private JsonObject createInfo(APIDefinition api) {
		JsonObject info = new JsonObject();
		
		info.addProperty("title", api.getTitle());
		info.addProperty("version", api.getVersion());
		
		return info;
	}
	
	private JsonArray createServers(String servers) {
		try {
			return gson.fromJson(servers, JsonArray.class);
		} catch (JsonSyntaxException e) {
			System.err.println("Could not parse servers object into JSON");
			return new JsonArray();
		}
	}
	
	private JsonArray getParameters(MethodDefinition mdef) {
		JsonArray parameters = new JsonArray();
		// Add path parameters
		for(String parameterName : mdef.getPathParameters().keySet()) {
			JsonObject parameter = new JsonObject();
			parameter.addProperty("name", parameterName);
			parameter.addProperty("in", "path");
			parameter.addProperty("required", true);
			
			parameter.add("schema", extractSchema(mdef.getPathParameters().get(parameterName)));
			
			String description = mdef.getParameterDescription(parameterName);
			if(description != null) {
				parameter.addProperty("description", description);
			}
			
			parameters.add(parameter);
		}
		
		return parameters;
	}
	
	private JsonObject getResponses(MethodDefinition mdef) {
		JsonObject root = new JsonObject();
		root.add("200", createDefaultResponse(mdef));
		return root;
	}
	
	private JsonObject getRequestBody(MethodDefinition mdef) {
		JsonObject root = new JsonObject();
		JsonObject schema = new JsonObject();
		
		schema.add("schema", extractSchema(mdef.getRequestBody()));
		
		root.addProperty("required", true);
		root.add("content", new JsonObject());
		root.get("content").getAsJsonObject().add(mdef.getConsumes(), schema);
		
		return root;
	}
	
	private JsonObject createDefaultResponse(MethodDefinition mdef) {
		JsonObject root = new JsonObject();
		JsonObject product = new JsonObject();
		if("application/json".equals(mdef.getProduces()) && mdef.getReturnType() != null) {
			product.add("schema", extractSchema(mdef.getReturnType()));
		}
		
		root.addProperty("description", "");
		root.add("content", new JsonObject());
		root.get("content").getAsJsonObject().add(mdef.getProduces(), product);
		
		return root;
	}
	
	private JsonElement extractSchema(TypeDefinition type) {
		JsonObject obj = null;
		
		// TODO: Use visitor pattern here instead
		if(type instanceof ClassDefinition) {
			obj = extractSchemaFromClass((ClassDefinition) type);
		} else if(type instanceof EnumDefinition) {
			obj = extractSchemaFromEnum((EnumDefinition) type);
		} else if(type instanceof ListDefinition) {
			obj = extractSchemaFromList((ListDefinition) type);
		} else if(type instanceof PrimitiveDefinition) {
			obj = extractSchemaFromPrimitive((PrimitiveDefinition) type);
		} else {
			return JsonNull.INSTANCE;
		}
		
		if(type.getDescription() != null) {
			obj.addProperty("description", type.getDescription());
		}
		
		return obj;
	}
	
	private JsonObject extractSchemaFromClass(ClassDefinition clazz) {
		JsonObject root = new JsonObject();
		JsonObject properties = new JsonObject();
		
		root.addProperty("type", "object");
		if(clazz.getDescription() != null) {
			root.addProperty("description", clazz.getDescription());
		}
		
		for(var child: clazz.getChildren().entrySet()) {
			properties.add(child.getKey(), extractSchema(child.getValue()));
		}
		
		root.add("properties", properties);
		return root;
	}
	
	private JsonObject extractSchemaFromEnum(EnumDefinition enu) {
		JsonObject root = new JsonObject();
		JsonArray constants = new JsonArray();
		
		root.addProperty("type", "string");
		
		if(enu.getDescription() != null) {
			root.addProperty("description", enu.getDescription());
		}
		
		for(String key: enu.getValues().keySet()) {
			constants.add(key);
		}
		
		root.add("enum", constants);
		return root;
	}
	
	private JsonObject extractSchemaFromList(ListDefinition list) {
		JsonObject root = new JsonObject();
		
		root.addProperty("type", "array");
		if(list.getDescription() != null) {
			root.addProperty("description", list.getDescription());
		}
		
		root.add("items", extractSchema(list.getSubType()));
		return root;
	}
	
	private JsonObject extractSchemaFromPrimitive(PrimitiveDefinition primitive) {
		JsonObject root = new JsonObject();
		switch(primitive.getType()) {
			case "byte":
			case "short":
			case "int":
			case "long":
				root.addProperty("type", "integer");
				break;
			case "boolean":
				root.addProperty("type", "boolean");
				break;
			case "float":
			case "double":
				root.addProperty("type", "number");
				break;
			case "char":
				root.addProperty("maxLength", 1);
			case "string":
				root.addProperty("type", "string");
				break;
		}
		
		return root;
	}
}
