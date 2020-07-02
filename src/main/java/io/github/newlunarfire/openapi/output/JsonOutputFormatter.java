package io.github.newlunarfire.openapi.output;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor9;

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
				
				pathVerbObj.addProperty("description", mdef.getBody());
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
	
	private JsonObject primitiveToSchema(String type) {
		JsonObject schema = new JsonObject();
		switch(type) {
			case "byte":
			case "short":
			case "int":
			case "long":
				schema.addProperty("type", "integer");
				break;
			case "boolean":
				schema.addProperty("type", "boolean");
				break;
			case "float":
			case "double":
				schema.addProperty("type", "number");
				break;
			case "char":
				schema.addProperty("type", "string");
				schema.addProperty("maxLength", 1);
				break;
		}
		
		return schema;
	}
	
	private JsonElement extractSchema(TypeMirror t) {
		return t.accept(new SimpleTypeVisitor9<JsonElement, Void>(JsonNull.INSTANCE) {
			@Override
			public JsonElement visitDeclared(DeclaredType t, Void p) {
		 		try {
					return extractSchema(getClass().getClassLoader().loadClass(t.toString()));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
		 		
		 		return JsonNull.INSTANCE;
		 	}
		 	
			@Override
			public JsonElement visitPrimitive(PrimitiveType t, Void p) {
				return primitiveToSchema(t.toString());
		 	}
		}, null);
	}
	
	private JsonElement extractSchema(Type t) {
		Class<?> c;
		JsonObject root = new JsonObject();
		
		if(t instanceof ParameterizedType) {
			c = (Class<?>) ((ParameterizedType) t).getRawType();
		} else if (t instanceof Class){
			c = (Class<?>) t;
		} else {
			return JsonNull.INSTANCE;
		}

 		if(c.isPrimitive()) {
 			return primitiveToSchema(c.getSimpleName());
		} else if(String.class.equals(c)) {
			root.addProperty("type", "string");
		} else if(List.class.equals(c)) {
			root.addProperty("type", "array");
			root.add("items", extractSchema(((ParameterizedType) t).getActualTypeArguments()[0]));
		} else if(c.isEnum()) {
			root.addProperty("type", "string");
			JsonArray enumConstants = new JsonArray();
			List.of(c.getEnumConstants()).stream()
				.map(Object::toString)
				.forEach(enumConstants::add);

			root.add("enum", enumConstants);
		} else {
			// POJO 
			JsonObject properties = new JsonObject();
			
			Arrays.stream(c.getMethods())
				.filter(_m -> _m.getName().startsWith("get") && !_m.getName().equals("getClass"))
				.forEach(_m -> {
					String name = _m.getName().substring(3);
					name = name.substring(0,1).toLowerCase() + name.substring(1);
					properties.add(name, extractSchema(_m.getGenericReturnType()));
				});
				
			root.addProperty("type", "object");
			root.add("properties", properties);
		}
		
		return root;
	}
}
