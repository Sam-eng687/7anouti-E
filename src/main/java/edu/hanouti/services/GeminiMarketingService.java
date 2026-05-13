package edu.hanouti.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * Gemini (Google AI) pour titres de campagne et assistant boutique.
 * Cle : variable GEMINI_API_KEY ou fichier ~/.hanouti/gemini.properties ; sinon cle par defaut embarquee (risque si le code est partage).
 */
public class GeminiMarketingService {

    public static final String DEFAULT_MODEL = "gemini-2.5-flash";

    /** Cle par defaut si aucune config externe (exposer le binaire ou le repo = exposer la cle). */
    private static final String FALLBACK_API_KEY = "AIzaSyBlB8nFdkmBgSbrU0EcEzvAG83RqrcBTas";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(25))
            .build();

    private volatile String cachedKey;
    private volatile String cachedModel = DEFAULT_MODEL;

    public GeminiMarketingService() {
        refreshConfig();
    }

    /** Recharge cle / modele depuis l'environnement ou ~/.hanouti/gemini.properties */
    public void refreshConfig() {
        String k = System.getenv("GEMINI_API_KEY");
        if (k == null || k.isBlank())
            k = System.getProperty("gemini.api.key", "");
        String m = System.getenv("GEMINI_MODEL");
        if (m == null || m.isBlank())
            m = System.getProperty("gemini.model", DEFAULT_MODEL);
        Path props = Path.of(System.getProperty("user.home"), ".hanouti", "gemini.properties");
        if (Files.isRegularFile(props)) {
            try (InputStream in = Files.newInputStream(props)) {
                Properties p = new Properties();
                p.load(in);
                String fk = p.getProperty("apiKey", "").trim();
                if (!fk.isBlank())
                    k = fk;
                String fm = p.getProperty("model", "").trim();
                if (!fm.isBlank())
                    m = fm;
            } catch (IOException ignored) {
            }
        }
        cachedKey = k != null ? k.trim() : "";
        if (cachedKey.isEmpty())
            cachedKey = FALLBACK_API_KEY;
        cachedModel = (m != null && !m.isBlank()) ? m.trim() : DEFAULT_MODEL;
    }

    public boolean hasApiKey() {
        refreshConfig();
        return cachedKey != null && !cachedKey.isBlank();
    }

    public String getModelId() {
        return cachedModel != null ? cachedModel : DEFAULT_MODEL;
    }

    /**
     * Ameliore / corrige un titre de campagne marketing (une ligne, sans guillemets).
     */
    public String polishCampaignTitle(String draftTitle, String objectif, String canal) throws IOException, InterruptedException {
        String draft = draftTitle == null ? "" : draftTitle.trim();
        if (draft.isEmpty())
            return "";
        String sys = "Tu es un correcteur orthographique. Corrige UNIQUEMENT les fautes d'orthographe, de grammaire et les mots mal ecrits dans le titre. "
                + "Ne change pas le sens, ne reformule pas, ne rends pas plus accrocheur. "
                + "Reponds avec UNE SEULE ligne : le titre corrige uniquement, sans guillemets, sans prefixe, max 90 caracteres.";
        String user = "Titre brut : " + draft + "\nObjectif campagne : " + nullToEmpty(objectif) + "\nCanal : "
                + nullToEmpty(canal);
        String raw = generateText(sys, user, 128, 0.35);
        return sanitizeSingleLineTitle(raw);
    }

    /**
     * Message utilisateur avec contexte conversation (texte concatene) pour l'assistant boutique.
     */
    public String chatShopAssistant(String conversationBlock) throws IOException, InterruptedException {
        String sys = "Tu es l'assistant Hanouti pour l'application desktop 7anouti Marketing (e-commerce : produits, "
                + "stocks, campagnes marketing, promos, bundles, statistiques, conseils IA). "
                + "Tu aides a utiliser l'app, a proposer des idees marketing / promos, et des conseils e-commerce generaux "
                + "(comme pour une boutique en ligne). Reste concis (souvent moins de 180 mots). "
                + "Si la question est totalement hors-sujet (ex. piratage, illegal), refuse poliment. "
                + "Reponds dans la meme langue que l'utilisateur (francais s'il ecrit en francais).";
        return generateText(sys, conversationBlock, 1024, 0.55);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String generateText(String systemInstruction, String userText, int maxOut, double temperature)
            throws IOException, InterruptedException {
        refreshConfig();
        if (cachedKey == null || cachedKey.isBlank())
            throw new IOException(
                    "Cle Gemini absente. Definis GEMINI_API_KEY ou cree le fichier .hanouti/gemini.properties dans ton dossier utilisateur (cle apiKey=...).");

        String model = (cachedModel == null || cachedModel.isBlank()) ? DEFAULT_MODEL : cachedModel.trim();
        String keyEnc = URLEncoder.encode(cachedKey, StandardCharsets.UTF_8);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key="
                + keyEnc;

        JsonObject body = new JsonObject();
        JsonObject sys = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sp = new JsonObject();
        sp.addProperty("text", systemInstruction);
        sysParts.add(sp);
        sys.add("parts", sysParts);
        body.add("systemInstruction", sys);

        JsonArray contents = new JsonArray();
        JsonObject turn = new JsonObject();
        turn.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject pt = new JsonObject();
        pt.addProperty("text", userText);
        parts.add(pt);
        turn.add("parts", parts);
        contents.add(turn);
        body.add("contents", contents);

        JsonObject gen = new JsonObject();
        gen.addProperty("maxOutputTokens", maxOut);
        gen.addProperty("temperature", temperature);
        body.add("generationConfig", gen);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new IOException("HTTP " + resp.statusCode() + " : " + truncate(resp.body(), 400));

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (root.has("error")) {
            String msg = root.getAsJsonObject("error").has("message")
                    ? root.getAsJsonObject("error").get("message").getAsString()
                    : resp.body();
            throw new IOException(msg);
        }
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.size() == 0)
            throw new IOException("Reponse vide du modele.");
        JsonObject first = candidates.get(0).getAsJsonObject();
        if (first.has("finishReason") && "SAFETY".equals(first.get("finishReason").getAsString()))
            throw new IOException("Reponse bloquee (safety). Reformule ta demande.");
        JsonObject content = first.getAsJsonObject("content");
        JsonArray p2 = content.getAsJsonArray("parts");
        if (p2 == null || p2.size() == 0)
            throw new IOException("Pas de texte dans la reponse.");
        return p2.get(0).getAsJsonObject().get("text").getAsString().trim();
    }

    private static String truncate(String s, int max) {
        if (s == null)
            return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String sanitizeSingleLineTitle(String raw) {
        if (raw == null)
            return "";
        String t = raw.replace("\r", " ").replace("\n", " ").trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))
            t = t.substring(1, t.length() - 1).trim();
        if (t.length() > 120)
            t = t.substring(0, 120).trim();
        return t;
    }
}
