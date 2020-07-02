package test.github.newlunarfire.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.newlunarfire.openapi.defs.MethodDefinition;

public class MethodDefinitionTest {
	@Test
	void testRemoveQuotesFromFieldsOnProducesAndConsumes() {
		MethodDefinition mdef = new MethodDefinition();
		mdef.setProduces("\"application/json\"");
		mdef.setConsumes("\"application/json\"");
		
		assertEquals("application/json", mdef.getProduces());
		assertEquals("application/json", mdef.getConsumes());
	}
}
