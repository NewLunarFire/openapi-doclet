package io.github.newlunarfire.openapi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ClassUtils {
	public static Class<?> findClass(String name) {
		return findClass("/", name);
	}
	
	public static Class<?> findClass(String pckage, String name) {
		List<String> classes = listPackage(pckage);
		
		if(classes.contains(name)) {
			try {
				ClassLoader cl = ClassUtils.class.getClassLoader();
				String className = pckage.substring(1).replace("/", ".") + name.substring(0, name.length() - 6);
				return cl.loadClass(className);
			} catch (ClassNotFoundException e) {
				System.err.println(e);
			} 
		} else {
			return classes.stream()
					.filter(sub -> !sub.endsWith(".class"))
					.map(sub -> findClass(pckage + sub + "/", name))
					.filter(cl -> cl != null)
					.findFirst()
					.orElse(null);
		}
		
		return null;
	}
	
	private static List<String> listPackage(String packageName) {
		InputStream is = ClassUtils.class.getResourceAsStream(packageName);
		if(is != null) {
			try {
				return List.of(new String(is.readAllBytes()).split("\n"));
			} catch (IOException e) {
				
			}
		}
		
		return List.of();
	}
}
