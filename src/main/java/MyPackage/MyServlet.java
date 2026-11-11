package MyPackage;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private WeatherApiClient weatherApiClient;
    private WeatherDatabase weatherDatabase;

    @Override
    public void init() throws ServletException {
        super.init();
        weatherApiClient = new WeatherApiClient();
        String webAppRoot = getServletContext().getRealPath("/");
        if (webAppRoot == null) {
            webAppRoot = System.getProperty("user.dir");
        }
        weatherDatabase = new WeatherDatabase(webAppRoot);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            request.getRequestDispatcher("/index.jsp").forward(request, response);
            return;
        }

        switch (action) {
            case "search":
                handleSearch(request, response);
                break;
            case "suggestions":
                handleSuggestions(request, response);
                break;
            case "geo":
                handleGeoLocation(request, response);
                break;
            case "favourites":
                handleFavouriteCities(request, response);
                break;
            case "history":
                handleSearchHistory(request, response);
                break;
            case "addFavourite":
                handleAddFavourite(request, response);
                break;
            case "removeFavourite":
                handleRemoveFavourite(request, response);
                break;
            case "clearHistory":
                handleClearHistory(request, response);
                break;
            default:
                writeJson(response, jsonError("Unsupported action: " + action));
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void handleSearch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String cityName = request.getParameter("city");
        if (cityName == null || cityName.trim().isEmpty()) {
            writeJson(response, jsonError("Please enter a valid city name."));
            return;
        }

        String normalizedCity = normalizeCityName(cityName);
        if (normalizedCity.isEmpty()) {
            writeJson(response, jsonError("Please enter a valid city name."));
            return;
        }

        try {
            JsonObject data = weatherApiClient.fetchWeatherByCity(normalizedCity);
            String city = data.has("city") ? data.get("city").getAsString() : normalizedCity;
            String country = data.has("country") ? data.get("country").getAsString() : "";
            weatherDatabase.addSearchHistory(city, country);
            writeJson(response, jsonSuccess(data));
        } catch (Exception ex) {
            writeJson(response, jsonError(ex.getMessage()));
        }
    }

    private void handleSuggestions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String cityName = request.getParameter("city");
        JsonArray suggestions = weatherApiClient.fetchCitySuggestions(cityName);
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.add("suggestions", suggestions);
        writeJson(response, responseJson);
    }

    private void handleGeoLocation(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String latitudeParam = request.getParameter("latitude");
        String longitudeParam = request.getParameter("longitude");

        try {
            double latitude = Double.parseDouble(latitudeParam);
            double longitude = Double.parseDouble(longitudeParam);
            JsonObject data = weatherApiClient.fetchWeatherByCoordinates(latitude, longitude);
            String city = data.has("city") ? data.get("city").getAsString() : "Current Location";
            weatherDatabase.addSearchHistory(city, data.has("country") ? data.get("country").getAsString() : "");
            writeJson(response, jsonSuccess(data));
        } catch (Exception ex) {
            writeJson(response, jsonError("Unable to get weather for your current location: " + ex.getMessage()));
        }
    }

    private void handleFavouriteCities(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonArray favourites = new JsonArray();
        List<String> cityNames = weatherDatabase.getFavouriteCities();
        for (String cityName : cityNames) {
            try {
                JsonObject cityWeather = weatherApiClient.fetchWeatherByCity(cityName);
                favourites.add(cityWeather);
            } catch (Exception ex) {
                JsonObject item = new JsonObject();
                item.addProperty("city", cityName);
                item.addProperty("status", "Unavailable");
                favourites.add(item);
            }
        }

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.add("favourites", favourites);
        writeJson(response, responseJson);
    }

    private void handleSearchHistory(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonArray history = new JsonArray();
        for (String item : weatherDatabase.getRecentSearches()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("label", item);
            String city = item;
            String country = "";
            int commaIndex = item.lastIndexOf(", ");
            if (commaIndex > 0) {
                city = item.substring(0, commaIndex).trim();
                country = item.substring(commaIndex + 2).trim();
            }
            entry.addProperty("city", city);
            entry.addProperty("country", country);
            history.add(entry);
        }

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.add("history", history);
        writeJson(response, responseJson);
    }

    private void handleAddFavourite(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String cityName = request.getParameter("city");
        String country = request.getParameter("country");
        String latitude = request.getParameter("latitude");
        String longitude = request.getParameter("longitude");

        String normalizedCity = normalizeCityName(cityName);
        if (normalizedCity.isEmpty()) {
            writeJson(response, jsonError("Please provide a city name."));
            return;
        }

        try {
            weatherDatabase.addFavourite(normalizedCity, country == null ? "" : country.trim(),
                    latitude == null || latitude.trim().isEmpty() ? 0.0 : Double.parseDouble(latitude),
                    longitude == null || longitude.trim().isEmpty() ? 0.0 : Double.parseDouble(longitude));
            writeJson(response, jsonSuccess(new JsonObject()));
        } catch (Exception ex) {
            writeJson(response, jsonError("Unable to save favourite city: " + ex.getMessage()));
        }
    }

    private void handleRemoveFavourite(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String cityName = request.getParameter("city");
        String normalizedCity = normalizeCityName(cityName);
        if (normalizedCity.isEmpty()) {
            writeJson(response, jsonError("City name is required."));
            return;
        }

        boolean removed = weatherDatabase.removeFavourite(normalizedCity);
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.addProperty("removed", removed);
        writeJson(response, responseJson);
    }

    private void handleClearHistory(HttpServletRequest request, HttpServletResponse response) throws IOException {
        weatherDatabase.clearSearchHistory();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", true);
        responseJson.addProperty("message", "Search history cleared.");
        writeJson(response, responseJson);
    }

    private String normalizeCityName(String cityName) {
        if (cityName == null) {
            return "";
        }

        String normalized = cityName.trim().replaceAll("\\s+", " ");
        if (normalized.endsWith(",")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private JsonObject jsonSuccess(JsonObject data) {
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.add("data", data);
        return json;
    }

    private JsonObject jsonError(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("success", false);
        json.addProperty("message", message);
        return json;
    }

    private void writeJson(HttpServletResponse response, JsonObject json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new Gson().toJson(json));
    }
}

