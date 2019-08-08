package me.coley.recaf;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.collect.Sets;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static java.util.Collections.*;

/**
 * Tests for workspace resource source code bindings.
 *
 * @author Matt
 */
public class SourceCodeTest extends Base {

	@Nested
	public class SourceUsage {
		private JavaResource resource;
		private Workspace workspace;

		@BeforeEach
		public void setup() {
			try {
				File file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
				if(!resource.setClassSources(file))
					fail("Failed to read sources!");
				workspace = new Workspace(resource);
			} catch(IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testNodeAtPos() {
			SourceCode code = resource.getClassSource("Start");
			// Line 7: Two tabs then this:
			//
			// Scanner scanner = new Scanner(System.in);
			//
			// First "Scanner" is an AST Tyoe
			Node node = code.getNodeAt(7, 5); // Scanner
			assertTrue(node instanceof ClassOrInterfaceType);
			assertEquals("Scanner", ((ClassOrInterfaceType)node).asString());
			// "scanner" is just a SimpleName, so we return the parent VariableDeclarator
			node = code.getNodeAt(7, 13); // scanner
			assertTrue(node instanceof VariableDeclarator);
			assertEquals("scanner", ((VariableDeclarator)node).getNameAsString());
			// Second "Scanner" is also an AST Type
			node = code.getNodeAt(7, 27); // Scanner
			assertTrue(node instanceof ClassOrInterfaceType);
			assertEquals("Scanner", ((ClassOrInterfaceType)node).asString());
			// "System.in" is a FieldAccessExpr
			// - "System" is a NameExpr - Field.scope
			// - "in" is a NameExpr - Field.name
			node = code.getNodeAt(7, 34); // System
			assertTrue(node instanceof FieldAccessExpr);
			assertTrue(((FieldAccessExpr)node).getScope() instanceof NameExpr);
			assertEquals("System", ((NameExpr)((FieldAccessExpr)node).getScope()).getNameAsString());
			assertEquals("in", ((FieldAccessExpr)node).getNameAsString());

		}

		@Test
		public void testNoImports() {
			List<String> imports = resource.getClassSource("calc/Expression").getImports();
			assertEquals(0, imports.size());
		}

		@Test
		public void testImpliedImports() {
			List<String> imports = resource.getClassSource("calc/Expression").getAllImports();
			assertEquals(8, imports.size() - SourceCode.LANG_PACKAGE_NAMES.length);
			for(String name : resource.getClasses().keySet())
				// Implied imports include classes in the same package
				// - of which there should be 8
				if(name.startsWith("calc/"))
					assertTrue(imports.contains(name));
		}

		@Test
		public void testExplicitImports() {
			List<String> imports = resource.getClassSource("calc/MatchUtil").getImports();
			// Imports only two classes
			assertEquals(2, imports.size());
			assertTrue(imports.contains("java/util/regex/Matcher"));
			assertTrue(imports.contains("java/util/regex/Pattern"));
		}

		@Test
		public void testWildcardImport() {
			List<String> imports = resource.getClassSource("Start").getImports();
			assertEquals(9, imports.size());
			for(String name : resource.getClasses().keySet())
				// Should have imported the entire package "calc.*"
				// which is all the remaining classes.
				if(name.startsWith("calc/"))
					assertTrue(imports.contains(name));
			// Also imports scanner
			assertTrue(imports.contains("java/util/Scanner"));
		}

		@Test
		public void testSurrounding() {
			// Test that the 5th line of the source file + a context radius of 1 line
			// matches the constructor of the given class.
			//
			// public Constant(int i) {
			//     super(i);
			// }
			String expected = "\tpublic Constant(int i) {\n\t\tsuper(i);\n\t}";
			String actual = resource.getClassSource("calc/Constant").getSurrounding(5, 1);
			assertEquals(expected, actual);
		}
	}

	@Nested
	public class SourceLoading {
		@Test
		public void testDefaultSourceLoading() {
			JavaResource resource;
			try {
				File file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
				if(!resource.setClassSources(file))
					fail("Failed to read sources!");
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertMatchingSource(resource);
		}

		@Test
		public void testSingleClassSourceLoading() {
			JavaResource resource;
			try {
				resource = new ClassResource(getClasspathFile("Hello.class"));
				resource.getClasses();
				if(!resource.setClassSources(getClasspathFile("Hello.java")))
					fail("Failed to read sources!");
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertMatchingSource(resource);
		}

		@Test
		public void testUrlDeferLoading() {
			JavaResource resource;
			try {
				resource = new UrlResource(getClasspathUrl("calc.jar"));
				resource.getClasses();
				if(!resource.setClassSources(getClasspathFile("calc.jar"))) {
					fail("Failed to read sources!");
				}
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertMatchingSource(resource);
		}

		@Test
		public void testSingleClassFailsOnJar() {
			JavaResource resource;
			try {
				resource = new ClassResource(getClasspathFile("Hello.class"));
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(getClasspathFile("calc.jar")));
		}

		@Test
		public void testJarFailsOnMissingFile() {
			JavaResource resource;
			try {
				File file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(new File("Does/Not/Exist")));
		}

		@Test
		public void testJarFailsOnBadFileType() {
			JavaResource resource;
			File source;
			try {
				File file = getClasspathFile("calc.jar");
				source = getClasspathFile("Hello.class");
				resource = new JarResource(file);
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(source));
		}
	}

	/**
	 * Asserts that the given resource's classes all have mappings to source files.<br>
	 * The given resource must not have any inner classes.
	 *
	 * @param resource
	 * 		Resource to check.
	 */
	private static void assertMatchingSource(JavaResource resource) {
		Set<String> expectedSrcNames = resource.getClasses().keySet();
		Set<String> foundSrcNames = resource.getClassSources().values().stream()
				.map(SourceCode::getInternalName).collect(Collectors.toSet());
		// Show that all classes (no inners in this sample) have source code mappings
		Set<String> difference = Sets.difference(expectedSrcNames, foundSrcNames);
		assertNotEquals(emptySet(), expectedSrcNames);
		assertNotEquals(emptySet(), foundSrcNames);
		assertEquals(emptySet(), difference);
	}
}
