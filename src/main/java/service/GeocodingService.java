package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeocodingService {

    public static double[] getCoordinates(String address) {

        try {

            String query = java.net.URLEncoder.encode(address, "UTF-8");

            String urlStr =
                    "https://nominatim.openstreetmap.org/search?q="
                            + query
                            + "&format=json&limit=1";

            URL url = new URL(urlStr);

            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            conn.setRequestProperty(
                    "User-Agent",
                    "FloodSimulation"
            );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()
                            )
                    );

            StringBuilder response = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            String json = response.toString();

            if (json.equals("[]")) {
                return null;
            }

            int latStart = json.indexOf("\"lat\":\"") + 7;
            int latEnd = json.indexOf("\"", latStart);

            int lonStart = json.indexOf("\"lon\":\"") + 7;
            int lonEnd = json.indexOf("\"", lonStart);

            double lat =
                    Double.parseDouble(
                            json.substring(latStart, latEnd)
                    );

            double lon =
                    Double.parseDouble(
                            json.substring(lonStart, lonEnd)
                    );

            return new double[] { lat, lon };

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
