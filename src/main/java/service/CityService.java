package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CityService {

    public static List<String> searchCities(String query) {
        List<String> cities = new ArrayList<>();

        if (query == null || query.trim().length() < 2) {
            return cities;
        }

        try {
            String url = "https://geo.api.gouv.fr/communes?nom="
                    + query.trim().replace(" ", "%20")
                    + "&fields=nom&boost=population&limit=10";

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();

            String[] parts = json.split("\"nom\":\"");

            for (int i = 1; i < parts.length; i++) {
                String city = parts[i].split("\"")[0];
                if (!cities.contains(city)) {
                    cities.add(city);
                }
            }

        } catch (Exception e) {
            System.out.println("[CityService] Erreur recherche villes : " + e.getMessage());
        }

        return cities;
    }
}
