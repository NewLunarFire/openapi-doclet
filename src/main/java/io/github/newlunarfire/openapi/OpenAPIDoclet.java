package io.github.newlunarfire.openapi;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;

import io.github.newlunarfire.openapi.defs.APIDefinition;
import io.github.newlunarfire.openapi.output.JsonOutputFormatter;

public class OpenAPIDoclet implements Doclet  {
	private final SimpleOption outputDirectoryOption = new SimpleOption(1, "Output Directory", Option.Kind.STANDARD, List.of("-d"), "directory");
	private final SimpleOption documentTitleOption = new SimpleOption(1, "Document title", Option.Kind.STANDARD, List.of("-doctitle"), "doctitle");
	private final SimpleOption versionOption = new SimpleOption(1, "API Version", Option.Kind.STANDARD, List.of("-apiVersion"), "apiVersion");
	private final SimpleOption apiServersOption = new SimpleOption(1, "API Servers", Option.Kind.STANDARD, List.of("-apiServers"), "apiServers");
	private final SimpleOption packageOption = new SimpleOption(1, "API Resource Packages", Option.Kind.STANDARD, List.of("-apiPackages"), "packages");

	@Override
	public void init(Locale locale, Reporter reporter) {
	}

	@Override
	public String getName() {
		return OpenAPIDoclet.class.getSimpleName();
	}

	@Override
	public Set<? extends Option> getSupportedOptions() {
		return Set.of(
			outputDirectoryOption,
			documentTitleOption,
			versionOption,
			apiServersOption,
			packageOption,
			new SimpleOption(1, "Window title", Option.Kind.STANDARD, List.of("-windowtitle"), "windowtitle")
		);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}

	@Override
	public boolean run(DocletEnvironment environment) {
		// Check options
		if(packageOption.getValue() == null) {
			return false;
		}
		
		var scanner = new OpenAPIScanner();
		
		scanner.setTitle(documentTitleOption.getValue());
		scanner.setVersion(versionOption.getValue());
		scanner.setServers(apiServersOption.getValue());
		scanner.setPackages(List.of(packageOption.getValue().split(";")));
		
		APIDefinition api = scanner.scan(environment);
		
		try {
			new JsonOutputFormatter(api).output(new FileOutputStream(Paths.get(outputDirectoryOption.getValue(), "openapi.json").toFile()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	private class SimpleOption implements Option {
		private final int argCount;
		private final String description;
		private final Kind kind;
		private final List<String> names;
		private final String parameters;
		private String value;

		public SimpleOption(int argCount, String description, Kind kind, List<String> names, String parameters) {	 
			this.argCount = argCount;
			this.description = description;
			this.kind = kind;
			this.names = names;
			this.parameters = parameters;
		}

		@Override
		public int getArgumentCount() {
			return argCount;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public Kind getKind() {
			return kind;
		}

		@Override
		public List<String> getNames() {
			return names;
		}

		@Override
		public String getParameters() {
			return parameters;
		}
		
		public String getValue() {
			return this.value;
		}

		@Override
		public boolean process(String option, List<String> arguments) {
			this.value = arguments.get(0);
			return true;
		}
	}
}