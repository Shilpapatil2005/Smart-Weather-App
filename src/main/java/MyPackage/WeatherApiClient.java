package MyPackage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WeatherApiClient {
    private static final String GEOCODING_BASE = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_BASE = "https://api.open-meteo.com/v1/forecast";
    private static final String AIR_QUALITY_BASE = "https://air-quality-api.open-meteo.com/v1/air-quality";

    private static String normalizeCityName(String cityName) {
        if (cityName == null) {
            return "";
        }

        String normalized = cityName.trim().replaceAll("\\s+", " ");
        if (normalized.endsWith(",")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    public JsonObject fetchWeatherByCity(String cityName) throws IOException {
        String query = normalizeCityName(cityName);
        if (query.isEmpty()) {
            throw new IOException("City name is required.");
        }

        String geocodingUrl = GEOCODING_BASE + "?name=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&count=10&language=en&format=json";
        JsonObject geoResponse = readJson(geocodingUrl);
        JsonArray results = geoResponse.has("results") ? geoResponse.getAsJsonArray("results") : new JsonArray();

        if (results.size() == 0) {
            throw new IOException("No matching city found for: " + query);
        }

        JsonObject selectedCity = selectBestGeocodingMatch(results, query);
        String city = selectedCity.has("name") ? normalizeCityName(selectedCity.get("name").getAsString()) : query;
        String country = selectedCity.has("country") ? selectedCity.get("country").getAsString() : "";
        return fetchWeather(
                selectedCity.get("latitude").getAsDouble(),
                selectedCity.get("longitude").getAsDouble(),
                city,
                country,
                selectedCity.has("timezone") ? selectedCity.get("timezone").getAsString() : ""
        );
    }

    public JsonObject fetchWeatherByCoordinates(double latitude, double longitude) throws IOException {
        String reverseGeocodingUrl = "https://geocoding-api.open-meteo.com/v1/reverse"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&language=en&format=json";

        JsonObject reverseResponse = readJson(reverseGeocodingUrl);
        JsonArray results = reverseResponse.has("results") ? reverseResponse.getAsJsonArray("results") : new JsonArray();

        String cityName = "Current Location";
        String countryName = "";
        String timezone = "";

        if (results.size() > 0) {
            JsonObject match = results.get(0).getAsJsonObject();
            if (match.has("name")) {
                cityName = normalizeCityName(match.get("name").getAsString());
            }
            if (match.has("country")) {
                countryName = match.get("country").getAsString();
            }
            if (match.has("timezone")) {
                timezone = match.get("timezone").getAsString();
            }
        }

        return fetchWeather(latitude, longitude, cityName, countryName, timezone);
    }

    public JsonArray fetchCitySuggestions(String query) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            return new JsonArray();
        }

        String geocodingUrl = GEOCODING_BASE + "?name=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
                + "&count=5&language=en&format=json";
        JsonObject geoResponse = readJson(geocodingUrl);
        JsonArray results = geoResponse.has("results") ? geoResponse.getAsJsonArray("results") : new JsonArray();
        JsonArray suggestions = new JsonArray();

        for (JsonElement element : results) {
            JsonObject city = element.getAsJsonObject();
            JsonObject suggestion = new JsonObject();
            suggestion.addProperty("name", city.get("name").getAsString());
            suggestion.addProperty("country", city.has("country") ? city.get("country").getAsString() : "");
            suggestion.addProperty("admin1", city.has("admin1") ? city.get("admin1").getAsString() : "");
            suggestion.addProperty("latitude", city.get("latitude").getAsDouble());
            suggestion.addProperty("longitude", city.get("longitude").getAsDouble());
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    private JsonObject fetchWeather(double latitude, double longitude, String cityName, String countryName, String timezone) throws IOException {
        String forecastUrl = FORECAST_BASE
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,pressure_msl,wind_speed_10m,wind_direction_10m,cloud_cover,visibility"
                + "&hourly=temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,sunrise,sunset"
                + "&timezone=auto&forecast_days=7&past_days=1";

        JsonObject forecast = readJson(forecastUrl);
        if (!forecast.has("current") || !forecast.has("hourly") || !forecast.has("daily")) {
            throw new IOException("Weather forecast data is unavailable for the selected location.");
        }

        JsonObject current = forecast.getAsJsonObject("current");
        JsonObject hourly = forecast.getAsJsonObject("hourly");
        JsonObject daily = forecast.getAsJsonObject("daily");

        JsonObject summary = new JsonObject();
        summary.addProperty("city", normalizeCityName(cityName));
        summary.addProperty("country", countryName == null ? "" : countryName.trim());
        summary.addProperty("timezone", forecast.has("timezone") ? forecast.get("timezone").getAsString() : timezone);
        summary.addProperty("latitude", latitude);
        summary.addProperty("longitude", longitude);
        summary.addProperty("temperature", round(current.get("temperature_2m").getAsDouble()));
        summary.addProperty("feelsLike", round(current.get("apparent_temperature").getAsDouble()));
        summary.addProperty("humidity", current.get("relative_humidity_2m").getAsInt());
        summary.addProperty("pressure", current.get("pressure_msl").getAsDouble());
        summary.addProperty("windSpeed", round(current.get("wind_speed_10m").getAsDouble()));
        summary.addProperty("windDirection", current.get("wind_direction_10m").getAsInt());
        summary.addProperty("visibility", roundToOneDecimal(current.get("visibility").getAsDouble() / 1000.0));
        summary.addProperty("cloudCover", current.get("cloud_cover").getAsInt());
        summary.addProperty("sunrise", getFirstValue(daily, "sunrise"));
        summary.addProperty("sunset", getFirstValue(daily, "sunset"));

        int weatherCode = hourly.has("weather_code") ? hourly.getAsJsonArray("weather_code").get(0).getAsInt() : 0;
        summary.addProperty("weatherCode", weatherCode);
        summary.addProperty("weatherDescription", describeWeatherCode(weatherCode));
        summary.addProperty("weatherIcon", getWeatherIcon(weatherCode));

        summary.add("hourlyForecast", buildHourlyForecast(hourly));
        summary.add("dailyForecast", buildDailyForecast(daily));

        JsonObject airQuality = fetchAirQuality(latitude, longitude);
        summary.add("airQuality", airQuality);

        JsonArray alerts = new JsonArray();
        summary.add("alerts", alerts);
        summary.addProperty("alertMessage", "No severe weather alerts are currently provided by the selected weather data source for this location.");

        return summary;
    }

    private JsonObject fetchAirQuality(double latitude, double longitude) throws IOException {
        String airUrl = AIR_QUALITY_BASE + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=us_aqi,pm2_5,pm10,nitrogen_dioxide,ozone,carbon_monoxide"
                + "&timezone=auto";
        JsonObject airResponse = readJson(airUrl);
        JsonObject current = airResponse.has("current") ? airResponse.getAsJsonObject("current") : new JsonObject();

        JsonObject airQuality = new JsonObject();
        int aqi = current.has("us_aqi") ? current.get("us_aqi").getAsInt() : 0;
        airQuality.addProperty("aqi", aqi);
        airQuality.addProperty("pm25", current.has("pm2_5") ? round(current.get("pm2_5").getAsDouble()) : 0);
        airQuality.addProperty("pm10", current.has("pm10") ? round(current.get("pm10").getAsDouble()) : 0);
        airQuality.addProperty("nitrogenDioxide", current.has("nitrogen_dioxide") ? round(current.get("nitrogen_dioxide").getAsDouble()) : 0);
        airQuality.addProperty("ozone", current.has("ozone") ? round(current.get("ozone").getAsDouble()) : 0);
        airQuality.addProperty("carbonMonoxide", current.has("carbon_monoxide") ? round(current.get("carbon_monoxide").getAsDouble()) : 0);
        airQuality.addProperty("category", describeAirQuality(aqi));
        airQuality.addProperty("status", getAirQualityStatus(aqi));
        return airQuality;
    }

    private JsonArray buildHourlyForecast(JsonObject hourly) {
        JsonArray result = new JsonArray();
        if (!hourly.has("time") || !hourly.has("temperature_2m") || !hourly.has("precipitation_probability")
                || !hourly.has("wind_speed_10m") || !hourly.has("weather_code")) {
            return result;
        }

        JsonArray times = hourly.getAsJsonArray("time");
        JsonArray temperatures = hourly.getAsJsonArray("temperature_2m");
        JsonArray rainChance = hourly.getAsJsonArray("precipitation_probability");
        JsonArray windSpeed = hourly.getAsJsonArray("wind_speed_10m");
        JsonArray weatherCodes = hourly.getAsJsonArray("weather_code");

        int limit = Math.min(12, times.size());
        for (int i = 0; i < limit; i++) {
            JsonObject item = new JsonObject();
            item.addProperty("time", times.get(i).getAsString());
            item.addProperty("temperature", round(temperatures.get(i).getAsDouble()));
            item.addProperty("rainChance", rainChance.get(i).getAsInt());
            item.addProperty("windSpeed", round(windSpeed.get(i).getAsDouble()));
            int code = weatherCodes.get(i).getAsInt();
            item.addProperty("weatherCode", code);
            item.addProperty("weatherDescription", describeWeatherCode(code));
            item.addProperty("weatherIcon", getWeatherIcon(code));
            result.add(item);
        }
        return result;
    }

    private JsonArray buildDailyForecast(JsonObject daily) {
        JsonArray result = new JsonArray();
        if (!daily.has("time") || !daily.has("temperature_2m_min") || !daily.has("temperature_2m_max")
                || !daily.has("precipitation_probability_max") || !daily.has("wind_speed_10m_max")
                || !daily.has("weather_code")) {
            return result;
        }

        JsonArray times = daily.getAsJsonArray("time");
        JsonArray minTemps = daily.getAsJsonArray("temperature_2m_min");
        JsonArray maxTemps = daily.getAsJsonArray("temperature_2m_max");
        JsonArray rainChance = daily.getAsJsonArray("precipitation_probability_max");
        JsonArray windSpeed = daily.getAsJsonArray("wind_speed_10m_max");
        JsonArray weatherCodes = daily.getAsJsonArray("weather_code");

        int limit = Math.min(7, times.size());
        for (int i = 0; i < limit; i++) {
            JsonObject item = new JsonObject();
            item.addProperty("date", times.get(i).getAsString());
            item.addProperty("minTemp", round(minTemps.get(i).getAsDouble()));
            item.addProperty("maxTemp", round(maxTemps.get(i).getAsDouble()));
            item.addProperty("rainChance", rainChance.get(i).getAsInt());
            item.addProperty("windSpeed", round(windSpeed.get(i).getAsDouble()));
            int code = weatherCodes.get(i).getAsInt();
            item.addProperty("weatherCode", code);
            item.addProperty("weatherDescription", describeWeatherCode(code));
            item.addProperty("weatherIcon", getWeatherIcon(code));
            result.add(item);
        }
        return result;
    }

    private JsonObject selectBestGeocodingMatch(JsonArray results, String query) {
        String expected = normalizeCityName(query).toLowerCase();
        JsonObject exactMatch = null;
        JsonObject countryMatch = null;

        for (JsonElement element : results) {
            JsonObject candidate = element.getAsJsonObject();
            String candidateName = normalizeCityName(candidate.has("name") ? candidate.get("name").getAsString() : "").toLowerCase();
            String candidateCountry = candidate.has("country") ? candidate.get("country").getAsString() : "";

            if (candidateName.equals(expected)) {
                return candidate;
            }
            if (candidateName.equals(expected.replaceAll("\\s+", ""))) {
                exactMatch = candidate;
            }
            if (candidateCountry != null && query.toLowerCase().contains(candidateCountry.toLowerCase())) {
                countryMatch = candidate;
            }
        }

        if (exactMatch != null) {
            return exactMatch;
        }
        if (countryMatch != null) {
            return countryMatch;
        }
        return results.get(0).getAsJsonObject();
    }

    private String getFirstValue(JsonObject container, String key) {
        JsonArray values = container.getAsJsonArray(key);
        return values == null || values.size() == 0 ? "N/A" : values.get(0).getAsString();
    }

    private String describeWeatherCode(int code) {
        if (code == 0) return "Clear sky";
        if (code <= 3) return "Partly cloudy";
        if (code <= 48) return "Foggy";
        if (code <= 67) return "Rainy";
        if (code <= 77) return "Snowy";
        if (code <= 82) return "Heavy rain";
        if (code <= 86) return "Heavy snow";
        if (code <= 99) return "Stormy";
        return "Weather alert";
    }

    private String getWeatherIcon(int code) {
        if (code == 0) return "☀️";
        if (code <= 3) return "⛅";
        if (code <= 48) return "🌫️";
        if (code <= 67) return "🌧️";
        if (code <= 77) return "❄️";
        if (code <= 82) return "🌧️";
        if (code <= 86) return "❄️";
        return "⛈️";
    }

    private String describeAirQuality(int value) {
        if (value <= 50) return "Good";
        if (value <= 100) return "Moderate";
        if (value <= 150) return "Unhealthy for sensitive groups";
        if (value <= 200) return "Unhealthy";
        if (value <= 300) return "Very unhealthy";
        return "Hazardous";
    }

    private String getAirQualityStatus(int value) {
        if (value <= 50) return "Air quality is good and safe for outdoor activity.";
        if (value <= 100) return "Air quality is moderate. Sensitive groups should reduce prolonged outdoor exposure.";
        if (value <= 150) return "Air quality is unhealthy for sensitive groups.";
        if (value <= 200) return "Air quality is unhealthy. Avoid long outdoor activities.";
        if (value <= 300) return "Air quality is very unhealthy. Limit outdoor activity.";
        return "Air quality is hazardous. Stay indoors and follow health guidance.";
    }

    private int round(double value) {
        return (int) Math.round(value);
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private JsonObject readJson(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/json");

        int statusCode = connection.getResponseCode();
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            throw new IOException("No response from weather API.");
        }

        StringBuilder builder = new StringBuilder();
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine());
            }
        } finally {
            connection.disconnect();
        }

        if (statusCode >= 400) {
            throw new IOException("Weather API request failed with status " + statusCode + ": " + builder);
        }

        String content = builder.toString();
        if (content == null || content.isEmpty()) {
            throw new IOException("Weather API returned an empty response.");
        }

        try {
            return new JsonParser().parse(content).getAsJsonObject();
        } catch (Exception ex) {
            throw new IOException("Failed to parse response from weather API: " + ex.getMessage(), ex);
        }
    }
}
