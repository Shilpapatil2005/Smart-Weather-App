package MyPackage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WeatherDatabase {
    private final String databaseUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final boolean mysqlEnabled;

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

    public WeatherDatabase(String webAppRootPath) {
        String mysqlUrl = System.getenv("WEATHER_DB_URL");
        String mysqlUser = System.getenv("WEATHER_DB_USER");
        String mysqlPassword = System.getenv("WEATHER_DB_PASSWORD");

        if (mysqlUrl != null && !mysqlUrl.trim().isEmpty()) {
            this.mysqlEnabled = true;
            this.databaseUrl = mysqlUrl.trim();
            this.databaseUser = mysqlUser == null ? "" : mysqlUser.trim();
            this.databasePassword = mysqlPassword == null ? "" : mysqlPassword;
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("MySQL JDBC driver not found. Add mysql-connector-j to WEB-INF/lib or leave WEATHER_DB_URL unset to use SQLite.", e);
            }
        } else {
            this.mysqlEnabled = false;
            this.databaseUser = "";
            this.databasePassword = "";
            File dataDir = new File(webAppRootPath, "WEB-INF/data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            File dbFile = new File(dataDir, "weather_app.db");
            this.databaseUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("SQLite JDBC driver not found", e);
            }
        }
        initialize();
    }

    private Connection openConnection() throws SQLException {
        if (mysqlEnabled) {
            return DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
        }
        return DriverManager.getConnection(databaseUrl);
    }

    public void initialize() {
        String favoritesSql = mysqlEnabled
                ? "CREATE TABLE IF NOT EXISTS favorites ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                    + "city_name VARCHAR(255) NOT NULL UNIQUE, "
                    + "country VARCHAR(255), "
                    + "latitude DOUBLE, "
                    + "longitude DOUBLE, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                : "CREATE TABLE IF NOT EXISTS favorites ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "city_name TEXT NOT NULL UNIQUE, "
                    + "country TEXT, "
                    + "latitude REAL, "
                    + "longitude REAL, "
                    + "created_at TEXT DEFAULT CURRENT_TIMESTAMP)";

        String historySql = mysqlEnabled
                ? "CREATE TABLE IF NOT EXISTS search_history ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                    + "city_name VARCHAR(255) NOT NULL, "
                    + "country VARCHAR(255), "
                    + "searched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                : "CREATE TABLE IF NOT EXISTS search_history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "city_name TEXT NOT NULL, "
                    + "country TEXT, "
                    + "searched_at TEXT DEFAULT CURRENT_TIMESTAMP)";

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(favoritesSql);
            statement.execute(historySql);
            if (!mysqlEnabled) {
                statement.execute("DELETE FROM search_history WHERE id NOT IN ("
                        + "SELECT MIN(id) FROM search_history GROUP BY LOWER(city_name), COALESCE(country, '')"
                        + ")");
                statement.execute("DELETE FROM favorites WHERE id NOT IN ("
                        + "SELECT MIN(id) FROM favorites GROUP BY LOWER(city_name)"
                        + ")");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void addFavourite(String cityName, String country, double latitude, double longitude) {
        String normalizedCity = normalizeCityName(cityName);
        if (normalizedCity.isEmpty()) {
            throw new IllegalArgumentException("City name is required.");
        }

        String countryValue = country == null ? "" : country.trim();

        try (Connection connection = openConnection()) {
            String selectSql = "SELECT id FROM favorites WHERE LOWER(city_name) = LOWER(?) LIMIT 1";
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setString(1, normalizedCity);
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        String updateSql = "UPDATE favorites SET country = ?, latitude = ?, longitude = ? WHERE id = ?";
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                            updateStatement.setString(1, countryValue);
                            updateStatement.setDouble(2, latitude);
                            updateStatement.setDouble(3, longitude);
                            updateStatement.setLong(4, id);
                            updateStatement.executeUpdate();
                        }
                        return;
                    }
                }
            }

            String insertSql = mysqlEnabled
                    ? "INSERT INTO favorites(city_name, country, latitude, longitude) VALUES (?, ?, ?, ?)"
                    : "INSERT INTO favorites(city_name, country, latitude, longitude) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setString(1, normalizedCity);
                insertStatement.setString(2, countryValue);
                insertStatement.setDouble(3, latitude);
                insertStatement.setDouble(4, longitude);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save favourite city", e);
        }
    }

    public boolean removeFavourite(String cityName) {
        String normalizedCity = normalizeCityName(cityName);
        if (normalizedCity.isEmpty()) {
            return false;
        }

        String sql = "DELETE FROM favorites WHERE LOWER(city_name) = LOWER(?)";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedCity);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove favourite city", e);
        }
    }

    public List<String> getFavouriteCities() {
        List<String> favourites = new ArrayList<>();
        String sql = "SELECT city_name FROM favorites ORDER BY created_at DESC";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                favourites.add(resultSet.getString("city_name"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read favourite cities", e);
        }
        return favourites;
    }

    public void addSearchHistory(String cityName, String country) {
        String normalizedCity = normalizeCityName(cityName);
        if (normalizedCity.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO search_history(city_name, country) "
                + "SELECT ?, ? WHERE NOT EXISTS ("
                + "SELECT 1 FROM search_history WHERE LOWER(city_name) = LOWER(?) AND COALESCE(country, '') = COALESCE(?, ''))";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedCity);
            statement.setString(2, country == null ? "" : country.trim());
            statement.setString(3, normalizedCity);
            statement.setString(4, country == null ? "" : country.trim());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save search history", e);
        }
    }

    public List<String> getRecentSearches() {
        List<String> searches = new ArrayList<>();
        String sql = "SELECT city_name, country FROM ("
                + "SELECT city_name, country, MAX(searched_at) AS latest_searched_at "
                + "FROM search_history "
                + "GROUP BY LOWER(city_name), COALESCE(country, '') "
                + ") ORDER BY latest_searched_at DESC LIMIT 10";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String city = resultSet.getString("city_name");
                String country = resultSet.getString("country");
                searches.add(country == null || country.isEmpty() ? city : city + ", " + country);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read search history", e);
        }
        return searches;
    }

    public void clearSearchHistory() {
        String sql = "DELETE FROM search_history";
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear search history", e);
        }
    }
}
