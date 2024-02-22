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
	    	Assert.fail("A span with an empty or missing name was found.");
	    }
	    
	    // Verify timestamp format
	    if (!span.has("timestamp") || !Pattern.matches("\\d+", span.get("timestamp").asText())) {
	    	Assert.fail("Invalid or missing timestamp for span.");
	    }
	    
	    // Check for expected span relationships
	    if (span.has("parentId") && span.get("parentId").asText().isEmpty()) {
	    	Assert.fail("Span has an empty parentId, indicating a broken parent-child relationship.");
	    }
	    
	 // Retrieve the span name
	    String spanName = span.get("name").asText();

	    // Check for a non-empty name
	    if (spanName.isEmpty()) {
	        LOGGER.error("Span name is empty.");
	        Assert.fail("A span with an empty name was found.");
	        return false;
	    }

	    // Check for basic method or constructor signature
	    boolean hasParentheses = spanName.contains("(") && spanName.contains(")");
	    boolean isConstructor = spanName.contains("<init>");
	    if (!hasParentheses && !isConstructor) {
	        LOGGER.error("Span name does not contain a valid method or constructor signature.");
	        Assert.fail("Span name does not contain a valid method or constructor signature: " + spanName);
	        return false;
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
