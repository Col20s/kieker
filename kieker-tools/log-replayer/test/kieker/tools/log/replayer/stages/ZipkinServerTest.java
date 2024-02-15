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
			Thread.sleep(5000); // Adjust the delay as needed

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
	            return true;
	        }

	     // Validate each trace and span according to expected structure and content
	        for (JsonNode trace : rootNode) {
	        	if (!trace.isArray() || trace.size() == 0) { // Replace isEmpty() with size() == 0
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
	        return true;
	    }
	    
	    // Verify timestamp format (e.g., "timestamp": "1609459200000")
	    if (!span.has("timestamp") || !span.get("timestamp").asText().matches("\\d+")) {
	        LOGGER.error("Invalid or missing timestamp for span.");
	        return true;
	    }
	    
	    // Check for expected span relationships (e.g., parent-child relationship via parentId)
	    if (span.has("parentId") && span.get("parentId").asText().isEmpty()) {
	        LOGGER.error("Span has an empty parentId, indicating a broken parent-child relationship.");
	        return true;
	    }
	    
	    // Verify expected span names based on Kieker data
	    Set<String> expectedSpanNames = new HashSet<>(Arrays.asList("spannames", "spanname"));
	    if (!expectedSpanNames.contains(span.get("name").asText())) {
	        LOGGER.error("Unexpected span name: " + span.get("name").asText());
	        return true;
	    }
	    
	    
	    // Check for specific tags that should exist based on replayed Kieker data
	    if (!span.has("tags") || !span.get("tags").has("environment") || 
	        !span.get("tags").get("environment").asText().equals("test")) {
	        LOGGER.error("Missing or incorrect 'environment' tag in span.");
	        return true;
	    }

	    // Check for specific span relationships, like spans related to specific services or operations
	    if (span.has("kind") && span.get("kind").asText().equals("CLIENT") &&
	        (!span.has("localEndpoint") || !span.get("localEndpoint").has("serviceName") ||
	        !span.get("localEndpoint").get("serviceName").asText().equals("expectedServiceName"))) {
	        LOGGER.error("Span does not have the expected service relationship.");
	        return true;
	    }

	    return true; 
	}

	
	@AfterEach
	public void stopZipkinServer() {
		process.destroyForcibly();
	}
}
