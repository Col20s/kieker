package kieker.tools.log.replayer.stages;

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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

			// Add a delay to allow Zipkin server to fully start
			Thread.sleep(1000); // Adjust the delay as needed

			waitForZipkinStartup();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void waitForZipkinStartup() throws IOException {
		// capture and print the process output
		InputStream inputStream = process.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			LOGGER.info(line);
			if (line.contains("Serving HTTP at")) {
				// Add an extra check to ensure the server is healthy
				if (checkZipkinHealth()) {
					LOGGER.info("Startup finished");
					return;
				} else {
					LOGGER.warn("Zipkin server is not healthy. Retrying...");
				}
			}
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

	private boolean checkZipkinForSpans() throws IOException {
		
		
	    // Zipkin API to check if traces were created
	    URL url = new URL("http://localhost:9411/api/v2/traces");
	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestMethod("GET");
	    
	 // Use the actual response code from the server
	    int responseCode = connection.getResponseCode();
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
	    

	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
	        JsonNode rootNode = objectMapper.readTree(response.toString());
	        if (!rootNode.isArray() || rootNode.size() == 0) { 
	            LOGGER.error("No traces found in Zipkin.");
	            return false;
	        }

	     // Validate each trace and span according to expected structure and content
	        for (JsonNode trace : rootNode) {
	        	if (!trace.isArray() || trace.size() == 0) { 
	                LOGGER.error("A trace with no spans was found.");
	                return false;
	            }
	            for (JsonNode span : trace) {
	                if (!isValidSpan(span)) {
	                    return false; // `isValidSpan` encapsulates all span validation logic
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
	        LOGGER.error("A span with an empty or missing name was found.");
	        return false;
	    }
	    
	    // Verify timestamp format
	    if (!span.has("timestamp") || !Pattern.matches("\\d+", span.get("timestamp").asText())) {
	        LOGGER.error("Invalid or missing timestamp for span.");
	        return false;
	    }
	    
	    // Check for expected span relationships
	    if (span.has("parentId") && span.get("parentId").asText().isEmpty()) {
	        LOGGER.error("Span has an empty parentId, indicating a broken parent-child relationship.");
	        return false;
	    }
	 // Pattern for validating method signatures
	 // Validate span name with regex
	    String methodSignaturePattern = "(public|private|protected)?\\s*(static)?\\s*([\\w.<>\\[\\]]+\\s+)?(([\\w$]+\\.)*[\\w$]+(\\$[\\d]+)?|<init>|lambda\\$[\\w$]+\\$[\\d]+)\\s*\\(((\\s*[\\w$.<>,\\[\\]]+\\s*,?)*?)\\)";

	    Pattern pattern = Pattern.compile(methodSignaturePattern);

	    String spanName = span.get("name").asText();
	    Matcher matcher = pattern.matcher(spanName);
	    if (!matcher.matches()) {
	        LOGGER.error("Span name does not match the expected signature pattern: " + spanName);
	        return false; // Should return false if validation fails
	    }
	    
	    // Validate 'ipv4' and 'serviceName' in 'localEndpoint'
	    if (!span.has("localEndpoint") || !span.get("localEndpoint").isObject()) {
	        LOGGER.error("Missing 'localEndpoint' object in span.");
	        return false;
	    }
	    JsonNode localEndpoint = span.get("localEndpoint");
	    if (!localEndpoint.has("ipv4") || localEndpoint.get("ipv4").asText().isEmpty()) {
	        LOGGER.error("Missing or incorrect 'ipv4' in 'localEndpoint'.");
	        return false;
	    }
	    if (!localEndpoint.has("serviceName") || localEndpoint.get("serviceName").asText().isEmpty()) {
	        LOGGER.error("Missing or incorrect 'serviceName' in 'localEndpoint'.");
	        return false;
	    }

	    // Validate 'traceId'
	    if (!span.has("traceId") || span.get("traceId").asText().isEmpty()) {
	        LOGGER.error("Missing 'traceId'.");
	        return false;
	    }

	    // If all validations pass
	    return true;
	}

	
	@AfterEach
	public void stopZipkinServer() {
		process.destroyForcibly();
	}
}
