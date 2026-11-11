# Final Test Report

## Project state
This project was audited and tested as a live Java Servlet + JSP weather dashboard using real Open-Meteo APIs. The app is deployed locally and served from Tomcat at `http://localhost:8080/WeatherApp/`.

## Completed features and fixes
- Real city search and geocoding normalized before lookup and storage.
- Duplicate search-history entries are prevented in the database layer.
- Favourite cities are saved through the Java backend into persistent storage.
- Date and weekday labels are generated from API values instead of hardcoded weekday names.
- Hourly forecast records are displayed and mapped from actual API response arrays.
- Weather analytics charts render using API-driven datasets.
- Air-quality values are pulled from the Open-Meteo air-quality API and displayed with standard AQI labels.
- Smart insights are based on actual conditions such as humidity, rainfall probability, heat, and air quality.
- Search, Save, My Location, Clear History, and Dark Mode flows were tested against the live servlet endpoints.
- Loading states and API error handling were tightened with explicit status messages.
- Servlet-to-JSP and frontend-to-servlet communication was validated for search, favourites, warnings, and history actions.

## Fixed issues
1. City normalization and geocoding
   - City strings are trimmed and normalized before being sent to Open-Meteo.
   - Repeated whitespace and trailing commas are removed.
   - The geocoder now prefers a strong exact-name match to avoid incorrect suffix variants.

2. Duplicate history entries
   - Search history is deduplicated using normalized city name + country comparisons.
   - The database layer prevents repeated entries from being added for the same city.

3. Favourite-city persistence
   - Favourites are stored in the database and not only in browser memory.
   - Duplicate favourites are prevented using normalized city comparisons.

4. Forecast dates and timezone handling
   - Forecast date labels and hourly times are derived from API timestamps.
   - Time formatting is aligned with the location timezone metadata when present.

5. Weather insight logic
   - Insight messages are generated from actual temperature, humidity, rainfall probability, wind, and AQI values.

6. Frontend error/loading states
   - Loading messages and errors are shown explicitly for failed API/service calls.

7. MySQL vs SQLite
   - MySQL support is optional and only enabled when `WEATHER_DB_URL` is set.
   - The actual runtime configuration in this environment uses SQLite by default; no working MySQL configuration was found in the project or environment.

## Test cases run
| Test | Result |
|---|---|
| Java compilation | Passed |
| App deployed to Tomcat | Passed |
| Root page served | Passed |
| Search API (`/MyServlet?action=search&city=London`) | Passed |
| Suggestions API | Passed |
| Geo API (`/MyServlet?action=geo&latitude=...&longitude=...`) | Passed |
| Favourite-fetch endpoint | Passed |
| History-fetch endpoint | Passed |
| Add favourite endpoint | Passed |
| Remove favourite endpoint | Passed |
| Clear history endpoint | Passed |
| SQLite DB validation | Passed |
| Default DB use without MySQL config | Confirmed |

## Actual results
- Java compile command completed successfully after rebuild.
- The app served a valid dashboard page from Tomcat.
- Search endpoint returned valid JSON with current conditions, daily forecast, hourly forecast, and AQI payload.
- Suggestions returned multiple valid geocoding candidates.
- Geolocation endpoint returned valid JSON for a known coordinate set.
- Favourites and search history endpoints returned valid JSON.
- SQLite database file contained live values for favourites and search history without duplicate rows in the active database state.
- No MySQL JDBC setup or connection string was active in the project configuration.

## Remaining limitations and known notes
- Browser automation for every possible click path was not fully simulated in this environment; the backend and API flows were validated directly and the UI was loaded in-browser to confirm page rendering.
- Weather alerts are only shown when the source provides them; otherwise the system displays an explicit unavailable notice, which is the correct behavior for a live API-driven app.
- Exact output values vary by current API conditions, as expected for real-time weather data.

## Required environment variables
Optional database configuration:
- `WEATHER_DB_URL` – MySQL JDBC URL, e.g. `jdbc:mysql://host:3306/weatherapp`
- `WEATHER_DB_USER` – MySQL username
- `WEATHER_DB_PASSWORD` – MySQL password

If these variables are unset, the app uses SQLite automatically.

## Database setup
### SQLite (default, active in this project)
- Database file location: `src/main/webapp/WEB-INF/data/weather_app.db` in the deployed app
- The app creates the `favorites` and `search_history` tables automatically when it starts.

### MySQL (optional)
- Add MySQL JDBC driver to the project runtime.
- Set `WEATHER_DB_URL`, `WEATHER_DB_USER`, and `WEATHER_DB_PASSWORD` before starting Tomcat.
- Ensure the target database exists and is reachable.

## Application startup instructions
1. Ensure Java JDK 17+ is installed.
2. Start Tomcat with the bundled project deployed to `apache-tomcat/apache-tomcat-10.1.61`.
3. Ensure the webapp is present under the Tomcat `webapps` folder.
4. Launch the app with the project compiled to `src/main/webapp/WEB-INF/classes`.
5. Open: `http://localhost:8080/WeatherApp/`

## Final verdict
The application is functioning as a live Java Servlet + JSP weather dashboard using real API data. The major integrity issues identified during the audit were corrected and validated against live endpoints. The active persistence configuration is SQLite unless MySQL is explicitly configured.
