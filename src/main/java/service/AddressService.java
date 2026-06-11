package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class AddressService {

    public static List<String> searchLyonAddresses(String query) {

        List<String> addresses = new ArrayList<>();

        try {

            String encodedQuery =
                    URLEncoder.encode(query + " Lyon", StandardCharsets.UTF_8);

            String urlString =
                    "https://api-adresse.data.gouv.fr/search/?q="
                            + encodedQuery
                            + "&limit=10";

            URL url = new URL(urlString);

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(connection.getInputStream())
                    );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            JSONObject json =
                    new JSONObject(response.toString());

            JSONArray features =
                    json.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {

                JSONObject properties =
                        features.getJSONObject(i)
                                .getJSONObject("properties");

                String address =
                        properties.optString("name", "").trim();

                if (!address.isBlank()) {
                    addresses.add(address);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return addresses;
    }
}