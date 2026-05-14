package projet.hanouti.wejden.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsService {

    private static final String API_URL = "https://newsdata.io/api/1/latest?apikey=pub_1a4f4a6a636146ccb343eef64557dda6&language=en";

    /**
     * Récupère les derniers titres de news depuis NewsData.io
     */
    public List<String> getLatestNewsTitles() {
        List<String> titles = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Extraction robuste du champ "title" avec gestion des quotes échappées
                Pattern pattern = Pattern.compile("\"title\":\"((?:\\\\.|[^\"\\\\])*)\"");
                Matcher matcher = pattern.matcher(body);
                
                while (matcher.find()) {
                    String title = matcher.group(1);
                    // Nettoyage des caractères échappés JSON (ex: \" -> ")
                    title = title.replace("\\\"", "\"")
                                 .replace("\\\\", "\\")
                                 .replace("\\/", "/");
                    if (!titles.contains(title)) {
                        titles.add(title);
                    }
                    if (titles.size() >= 12) break; 
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des news: " + e.getMessage());
        }


        
        return titles;
    }
}
