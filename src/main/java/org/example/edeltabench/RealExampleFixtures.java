package org.example.edeltabench;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Original, unchanged Edelta sources and source Ecore files from the user's
 * research-example archive. The manifest preserves their original paths.
 */
public final class RealExampleFixtures {
	private static final String ROOT = "/real-examples/source-archive/";
	private static final String ECORE_SUPPORT_PATH = "/real-examples/support/EcoreForTests.ecore";
	private static final Pattern ECORE_IMPORT = Pattern.compile(
			"(?m)^\\s*metamodel\\s+\"ecore\"(?:\\s|$)");
	private static final byte[] ECORE_SUPPORT = read(ECORE_SUPPORT_PATH);
	private static final Map<String, Example> EXAMPLES = loadManifest();

	private RealExampleFixtures() { }

	public record ModelResource(String path, byte[] contents) { }

	public record Example(String id, String programPath, byte[] program,
			List<ModelResource> models, String expectedPackage, String category,
			boolean importsEcore) { }

	/** The full Ecore metamodel used by Edelta's own compiler tests. */
	static byte[] ecoreSupportModel() {
		return ECORE_SUPPORT;
	}

	public static List<Example> all() {
		return List.copyOf(EXAMPLES.values());
	}

	public static Example get(String id) {
		Example example = EXAMPLES.get(id);
		if (example == null) {
			throw new IllegalArgumentException("Unknown example '" + id + "'; available: "
					+ EXAMPLES.keySet());
		}
		return example;
	}

	private static Map<String, Example> loadManifest() {
		Map<String, Example> result = new LinkedHashMap<>();
		try (InputStream stream = RealExampleFixtures.class.getResourceAsStream(
				"/real-examples/cases.tsv")) {
			if (stream == null) {
				throw new IllegalStateException("Missing real-examples/cases.tsv");
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.startsWith("#")) {
						continue;
					}
					String[] columns = line.split("\\t", -1);
					if (columns.length != 5) {
						throw new IllegalStateException("Invalid example manifest line: " + line);
					}
					List<ModelResource> models = Arrays.stream(columns[2].split(";"))
							.map(path -> new ModelResource(path, read(ROOT + path)))
							.toList();
					byte[] program = read(ROOT + columns[1]);
					boolean importsEcore = ECORE_IMPORT.matcher(
							new String(program, StandardCharsets.UTF_8)).find();
					Example example = new Example(columns[0], columns[1],
							program, models, columns[3], columns[4], importsEcore);
					if (result.putIfAbsent(example.id(), example) != null) {
						throw new IllegalStateException("Duplicate example id: " + example.id());
					}
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not read real-example manifest", e);
		}
		if (result.isEmpty()) {
			throw new IllegalStateException("Real-example manifest is empty");
		}
		return Collections.unmodifiableMap(result);
	}

	private static byte[] read(String name) {
		try (InputStream in = RealExampleFixtures.class.getResourceAsStream(name)) {
			if (in == null) {
				throw new IllegalStateException("Missing original example resource: " + name);
			}
			return in.readAllBytes();
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + name, e);
		}
	}
}
