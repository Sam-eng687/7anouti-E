package org.example.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GeoService {

    public static String searchZone(String query) throws Exception {
        String encoded = URLEncoder.encode(query + ", Tunisia", StandardCharsets.UTF_8);

        String urlStr = "https://nominatim.openstreetmap.org/search"
                + "?q=" + encoded
                + "&format=json"
                + "&limit=5"
                + "&addressdetails=1";

        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "7anouti-E-JavaFX/1.0");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();
        return response.toString();
    }
    public static String reverseGeocode(double lat, double lon) throws Exception {
        String urlStr = "https://nominatim.openstreetmap.org/reverse"
                + "?lat=" + lat
                + "&lon=" + lon
                + "&format=json";

        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "7anouti-E-JavaFX/1.0");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();
        return response.toString();
    }
}