package kieker.tools.log.replayer.stages;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kieker.tools.log.replayer.ReplayerMain;

//import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZipkinServerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinServerTest.class);

	private static final String jarPath = "src/test/resources/zipkin-server-3.0.4-exec.jar";

	private static final String kiekerDataPath = "src/test/resources/kieker_results";

	// private static final String kiekerDataPath =
	// "src/test/resources/kieker-20231110-131058-869910315387-UTC--KIEKER";

	private Process process;

	@BeforeEach
	public void startZipkinServer() throws IOException {
		LOGGER.info("Starting Zipkin");

		String command = String.format("java -jar %s", jarPath);
		try {
			LOGGER.info("Command: " + command);
			process = Runtime.getRuntime().exec(command);

			waitForZipkinStartup();

			if (!process.isAlive()) {
				throw new RuntimeException("Zipkin did not start up correctly");
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void waitForZipkinStartup() throws IOException, InterruptedException {
		// capture and print the process output

		StringBuffer standardOut = new StringBuffer();
		StreamGobbler stdoutStreamThread = new StreamGobbler(process.getInputStream(), true, standardOut);
		StreamGobbler stderrStreamThread = new StreamGobbler(process.getErrorStream(), true, standardOut);
		
		stdoutStreamThread.start();
		stderrStreamThread.start();

		for (int i = 0; i < 10 && process.isAlive(); i++) {
			if (standardOut.toString().contains("Serving HTTP at")) {
				if (checkZipkinHealth()) {
					LOGGER.info("Startup finished");
					return;
				} else {
					LOGGER.warn("Zipkin server is not healthy. Retrying...");
				}
			}
			Thread.sleep(5000);
		}
	}

	private boolean checkZipkinHealth() throws IOException {
		// Zipkin health check to ensure the server is ready
		URL url = new URL("http://localhost:9411/health");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		int responseCode = connection.getResponseCode();
		return responseCode == HttpURLConnection.HTTP_OK;
	}

	@Test
	public void test() throws IOException {
		try {
			// Get list of files in the kieker-data directory
			File[] kiekerDataFiles = Objects.requireNonNull(new java.io.File(kiekerDataPath).listFiles());

			for (File kiekerDataFile : kiekerDataFiles) {
				// Replay each Kieker data file

				String kiekerFolderPath = kiekerDataFile.listFiles()[0].getAbsolutePath();
				System.out.println(kiekerFolderPath);

				ReplayerMain main = new ReplayerMain();
				main.run("Replayer", "replayer", new String[] { "--no-delay", "-i", kiekerFolderPath });

				// Check Zipkin API for spans
				boolean spansCreated = checkZipkinForSpans();
				assertTrue(spansCreated, "Spans should be created in Zipkin");
				boolean spansCreated1 = checkZipkinForSpans();
				assertTrue(spansCreated1, "Spans should be created in Zipkin with correct signatures and relationships.");

			}
			
			Thread.sleep(1000); // Sleep to manually check zipkin

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean checkZipkinForSpans() throws IOException,  InterruptedException {
		
		Thread.sleep(10000);
		
	    // Zipkin API to check if traces were created
	    URL url = new URL("http://localhost:9411/api/v2/traces");
	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestMethod("GET");
	    
	 // Use the actual response code from the server
	    int responseCode = connection.getResponseCode();
	    LOGGER.info("Zipkin server response code: " + responseCode);
	    if (responseCode != HttpURLConnection.HTTP_OK) {
	        LOGGER.error("Received HTTP error code from Zipkin server: " + responseCode);
	        return false;
	    }

	   
	    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	    StringBuilder response = new StringBuilder();
	    String inputLine;
	    while ((inputLine = in.readLine()) != null) {
	        response.append(inputLine);
	    }
	    in.close();
	    LOGGER.info("Zipkin traces response: " + response);

	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
	        JsonNode rootNode = objectMapper.readTree(response.toString());
	        if (!rootNode.isArray() || rootNode.size() == 0) { 
	            Assert.fail("No traces found in Zipkin.");
	        }

	     // Validate each trace and span according to expected structure and content
	        for (JsonNode trace : rootNode) {
	        	if (!trace.isArray() || trace.size() == 0) { 
	                LOGGER.error("A trace with no spans was found.");
	                return false;
	            }
	            for (JsonNode span : trace) {
	                if (!isValidSpan(span)) {
	                    return false; 
	                }
	            }
	        }
	    } catch (IOException e) {
	        LOGGER.error("IOException during Zipkin span check", e);
	        return false;
	    } catch (Exception e) {
	        LOGGER.error("Unexpected exception during Zipkin span check", e);
	        return false;
	    }
	    return true; 
	}

	private boolean isValidSpan(JsonNode span) {
	    // Check for a non-empty name
	    if (!span.has("name") || span.get("name").asText().isEmpty()) {
	        LOGGER.error("Span name is empty.");
	        Assert.fail("A span with an empty or missing name was found.");
	        return false;
	    }

	    // Verify timestamp format
	    String timestamp = span.get("timestamp").asText();
	    if (!timestamp.matches("\\d+")) {
	        LOGGER.error("Invalid or missing timestamp for span.");
	        Assert.fail("Invalid or missing timestamp for span.");
	        return false;
	    }

	    // Check for expected span relationships
	    if (span.has("parentId") && span.get("parentId").asText().isEmpty()) {
	        LOGGER.error("Span has an empty parentId, indicating a broken parent-child relationship.");
	        Assert.fail("Span has an empty parentId, indicating a broken parent-child relationship.");
	        return false;
	    }

	    // Validate span name structure and content
	    String spanName = span.get("name").asText().trim();
	    boolean containsParentheses = spanName.contains("(") && spanName.contains(")");
	    boolean isConstructor = spanName.contains("<init>");
	    boolean validSignature = isValidMethodOrConstructorSignature(spanName) || isConstructor;
	    if (!containsParentheses && !isConstructor) {
	        LOGGER.error("Span name does not contain a valid method or constructor signature: " + spanName);
	        Assert.fail("Span name does not contain a valid method or constructor signature.");
	        return false;
	    }

	    //checks for method signature
	    if (!isConstructor) {
	        // Check for space before opening parenthesis for methods
	        int parenIndex = spanName.indexOf('(');
	        if (parenIndex <= 0 || spanName.substring(0, parenIndex).endsWith(" ")) {
	            LOGGER.error("Invalid method signature: " + spanName);
	            Assert.fail("Invalid method signature.");
	            return false;
	        }
	    }

	    // Validate 'ipv4' and 'serviceName' in 'localEndpoint'
	    JsonNode localEndpoint = span.get("localEndpoint");
	    if (localEndpoint == null || !localEndpoint.has("ipv4") || localEndpoint.get("ipv4").asText().isEmpty() ||
	        !localEndpoint.has("serviceName") || localEndpoint.get("serviceName").asText().isEmpty()) {
	        LOGGER.error("Missing or incorrect 'ipv4' or 'serviceName' in 'localEndpoint'.");
	        return false;
	    }

	    // Validate 'traceId'
	    if (!span.has("traceId") || span.get("traceId").asText().isEmpty()) {
	        LOGGER.error("Missing 'traceId'.");
	        return false;
	    }

	    // If all validations pass
	   // LOGGER.info("Span name and related properties are valid.");
	    return true;
	}
	
	private boolean isValidMethodOrConstructorSignature(String spanName) {
	    // Initial check for basic structure
	    if (!spanName.contains("(") || !spanName.contains(")")) return false;

	    // Handling lambda expressions and anonymous classes
	    if (spanName.contains("lambda$") || spanName.contains("$")) {
	        return containsSpecialMethodName(spanName);
	    }

	    // Splitting the span name to identify components
	    String[] components = spanName.split("\\s+");

	    // Detecting visibility modifiers or static keyword
	    boolean startsWithModifier = spanName.matches("(public|private|protected|static)\\s+.*");

	    // Constructor validation
	    boolean isConstructor = spanName.contains("<init>");
	    if (isConstructor) {
	        return validateConstructor(components);
	    }

	    // Method validation, including static methods
	    return validateMethod(components, spanName, startsWithModifier);
	}

	private boolean validateConstructor(String[] components) {
	    // Check if the first component is a visibility modifier or a class name
	    boolean startsWithVisibilityModifier = components[0].matches("public|private|protected");
	    
	    // check the preceding class name.
	    int expectedInitIndex = startsWithVisibilityModifier ? 2 : 1;
	    boolean validStructure = components.length >= expectedInitIndex && components[expectedInitIndex - 1].equals("<init>");

	    if (!validStructure) {
	        return false; 
	    }

	    //Validate class name for constructors with visibility modifier
	    if (startsWithVisibilityModifier && !isValidClassName(components[1])) {
	        return false; 
	    }

	    if (components.length > expectedInitIndex) {
	        String parameters = extractParameters(components[expectedInitIndex]);
	        if (!validateParameters(parameters)) {
	            return false; 	        }
	    }

	    return true; 
	}

	private boolean isValidClassName(String className) {
	    // class name validation logic
	    // check class names follow Java identifier rules and contain periods for package separation
	    return className.matches("[a-zA-Z_$][a-zA-Z\\d_$]*(\\.[a-zA-Z_$][a-zA-Z\\d_$]*)*");
	}

	private String extractParameters(String component) {
	    // Extract parameter string from component, assuming it follows "<init>(parameters)" format
	    int startIndex = component.indexOf('(') + 1;
	    int endIndex = component.indexOf(')');
	    if (startIndex > 0 && endIndex > startIndex) {
	        return component.substring(startIndex, endIndex);
	    }
	    return ""; 
	}

	private boolean validateParameters(String parameters) {
	   
	    if (parameters.isEmpty()) {
	        return true; 
	    }
	    //pattern check for parameters (extremely simplified)
	    return parameters.matches("([a-zA-Z_$][a-zA-Z\\d_$]*(\\.[a-zA-Z_$][a-zA-Z\\d_$]*)*(,\\s*)?)+");
	}

	private boolean validateMethod(String[] components, String spanName, boolean startsWithModifier) {
	    int parenIndex = spanName.indexOf('(');
	    if (parenIndex <= 0) return false;
	    
	    if (startsWithModifier && !Character.isWhitespace(spanName.charAt(parenIndex - 1))) return false;
	    
	    // Checking if the part before the first parenthesis can be a valid method name or return type
	    String methodNameOrReturnType = spanName.substring(0, parenIndex).trim();
	    return methodNameOrReturnType.matches("[\\w.$]+");
	}

	private boolean containsSpecialMethodName(String spanName) {
	    // Checks for lambda expressions, anonymous classes, or special static method patterns
		return spanName.contains("lambda$") || 
		           spanName.matches(".+\\$\\d+\\..+") || 
		           spanName.contains("static ");
	}


	
	@AfterEach
	public void stopZipkinServer() {
		process.destroyForcibly();
	}
}
