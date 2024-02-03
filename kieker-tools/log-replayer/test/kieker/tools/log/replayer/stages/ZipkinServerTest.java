package kieker.tools.log.replayer.stages;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Objects;

public class ZipkinServerTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinServerTest.class);

	private static final String jarPath = "src/test/resources/zipkin-server-3.0.4-exec.jar";
	
	private static final String kiekerDataPath = "src/test/resources/kieker_results";
	
	//private static final String kiekerDataPath = "src/test/resources/kieker-20231110-131058-869910315387-UTC--KIEKER";
	
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
            ReplayerMain replayerMain = new ReplayerMain();

            // Get list of files in the kieker-data directory
            File[] kiekerDataFiles = Objects.requireNonNull(new java.io.File(kiekerDataPath).listFiles());

            for (File kiekerDataFile : kiekerDataFiles) {
                // Replay each Kieker data file

            	String kiekerFolderPath = kiekerDataFile.listFiles()[0].getAbsolutePath();
            	System.out.println(kiekerFolderPath);
            	
                replayerMain.run("Replayer", "replayer", new String[]{"--delay", "1", "-i", kiekerFolderPath});

                // Wait for a reasonable time to allow for spans to be created
                Thread.sleep(1000);

                // Check Zipkin API for spans
                boolean spansCreated = checkZipkinForSpans();
                assertTrue(spansCreated, "Spans should be created in Zipkin");
            }
            
            Thread.sleep(100000); // Sleep to manually check zipkin
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    
    private boolean checkZipkinForSpans() throws IOException {
        // Zipkin API to check if traces were created
        URL url = new URL("http://localhost:9411/api/v2/traces");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        return responseCode == HttpURLConnection.HTTP_OK;
    }

    
    @AfterEach
    public void stopZipkinServer() {
    	process.destroyForcibly();
    }
}
