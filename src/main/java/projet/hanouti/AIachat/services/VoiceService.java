package projet.hanouti.AIachat.services;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Bridges JavaFX to the local Whisper STT server running on localhost:5050.
 */
public class VoiceService {

    private static final String BASE_URL = "http://localhost:5050";
    private static final int HEALTH_TIMEOUT = 2;
    private static final int START_TIMEOUT = 5;
    private static final int STOP_TIMEOUT = 40;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HEALTH_TIMEOUT))
            .build();

    public boolean isServerRunning() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/health"))
                    .timeout(Duration.ofSeconds(HEALTH_TIMEOUT))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void startRecording() throws VoiceException {
        try {
            post("/start", START_TIMEOUT);
        } catch (Exception e) {
            throw new VoiceException("Could not start recording: " + e.getMessage(), e);
        }
    }

    public String stopAndTranscribe() throws VoiceException {
        try {
            String body = post("/stop", STOP_TIMEOUT);
            JSONObject json = new JSONObject(body);

            if (json.has("error")) {
                String err = json.optString("error", "unknown");
                if ("audio_too_short".equals(err)) return "";
                throw new VoiceException("Transcription error: " + err, null);
            }

            return json.optString("text", "").trim();
        } catch (VoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new VoiceException("Could not stop/transcribe: " + e.getMessage(), e);
        }
    }

    private String post(String path, int timeoutSeconds) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("HTTP " + resp.statusCode() + " from " + path);
        }
        return resp.body();
    }

    public static class VoiceException extends Exception {
        public VoiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}


