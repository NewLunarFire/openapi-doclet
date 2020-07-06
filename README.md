# OpenAPI 3.0 Doclet (Javadoc 11 compatible)

## Disclaimer

I make no guarantees about this project's suitability for your purposes and this is reflected in the GPLv3 license this project is released under. I am open to and welcome  suggestions and contributions, however I make no promise to fulfill or respond to any of them.

## Introduction

This project is a doclet that generates an OpenAPI specification from Java source, annotations and Javadoc comments. It looks at Java WS annotations to find the API Routes and inspects them to document them the best it can. What cannot be documented through annotations and « reflection » is done through Javadoc comments and tags.

## Notice

This project uses semantic versioning. It is still at version 0, meaning breaking changes could and will occur frequently until version 1. If you need stability, it is reccomended you pin a specific version and review changes before upgrading.

## Quick Start with Gradle

You can add the following Gradle task to your `build.gradle` file:

```groovy
repositories {
    ...
    maven {
		url  "https://tommysavaria.bintray.com/openapi-doclet"
	}
}

configurations {
    openapi
}

dependencies {
    ...
    openapi 'io.github.newlunarfire:openapi-doclet:0.0.0'
}

task generateDoc(type: Javadoc) {
	dependsOn classes
	group "documentation"
	description "Generate OpenAPI documentation"
	source sourceSets.main.allJava
	classpath = sourceSets.main.compileClasspath
	options.addStringOption("apiVersion", rootProject.version)
	options.addStringOption("apiServers", '[{"url":"http://api.example.com/","description":"You API Server "}]')
	options.addStringOption("apiPackages", "com.myapi.resources:com.anotherapi.resources")
	options.docTitle = "Your API"
	options.docletpath = sourceSets.main.output.classesDirs.asType(List)
	options.docletpath += new File(configurations.openapi.asPath)
	options.doclet = "io.github.newlunarfire.openapi.OpenAPIDoclet"
	options.noTimestamp(false)
	outputs.upToDateWhen { false }
}
```

This rather lengthy task description contains all the necessary flags to successfully invoke the doclet. Here are the important customizable parts: 
* `apiVersion` should be your api version. It should be in the `rootProject.version` variable, bit if it is not you may change this line. 
* `apiServers` is a JSON array containing you servers descriptions, as specified by the OpenAPI specification on Server Objects: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#serverObject
* `apiPackages` is a list of colon-separated fully-qualified package names to scan for API resources. The tool will scan all the classes in those packages for API endpoints.
* `docTitle` is the title of the openAPI document. You should name this with your API name.

❗ I wish to someday distribute this as a simple plug-and-play Gradle task, as well as a Maven (and possibly Ant) task, so any help is welcome.

## Command-Line options

| Flag          | Title                | Description                                         |
|---------------|----------------------|-----------------------------------------------------|
| -d            | Output Directory     | Path to output the file to                          |
| -doctitle     | Document Title       | Title of the document                               |
| -apiVersion   | API Version          | Version Number of your API                          |
| -apiServers   | API Servers          | Servers for your API                                |
| -apiPackages  | API Packages         | Packages to scan for API endpoints                  |
| -windowtitle  | UNUSED: Window Title | Provided for compatibility with Gradle Javadoc task |

## Contributing

You can freely contribute to this project. It is reccomended that you first fork the repository under your Github account, and make the changes there. You may then open a Pull Request to this repository.

❗ To easily test your changes, publish the changes to your local Maven repository with the `publishToMavenLocal` task, and add `mavenLocal()` to the repositories section of the target project. Also, you can suffix the version in this project with `-SNAPSHOT` to make sure Gradle always pulls the latest revision of your code.

## Feature Requests / Bug Fixes

You can open an Issue on Github to request a feature or to report a bug. However, please check that your issue is not in the Wishlist, as those are the issues that I plan to fix first and are probably already on the way.

## Wishlist

- Pusblish as a simple-to-use Gradle Task
- Support YAML output
- Support tags