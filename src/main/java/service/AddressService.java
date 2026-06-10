package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AddressService {

    public static List<String> searchLyonAddresses(String query) {
        List<String> results = new ArrayList<>();

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

            String urlString =
                    "https://api-adresse.data.gouv.fr/search/?" +
                    "q=" + encoded +
                    "&citycode=69123" +
                    "&limit=8" +
                    "&autocomplete=1";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            reader.close();

            String text = json.toString();

            String[] parts = text.split("\"label\":\"");

            for (int i = 1; i < parts.length; i++) {
                String label = parts[i].split("\"")[0];

                if (label.toLowerCase().contains("lyon")) {
                    results.add(label);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
