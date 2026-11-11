# Interview Preparation Guide

## 1. Project Explanation in Simple English
This project is a live weather dashboard built with Java Servlet and JSP. It queries real Open-Meteo APIs for geocoding, weather, and air quality, then renders the results in a dashboard with hourly and seven-day forecasts, analytics, favourites, recent history, and theme support. The app is intentionally a real-data, not mock-data, system.

## 2. Why I Developed This Project
I continued and stabilised an existing Java web app to make it production-like, real-data-driven, and robust enough for live demonstration. The key goals were to fix backend logic, validate real API behaviour, ensure persistence is honest, and keep the original architecture intact without redesigning the app.

## 3. Problem It Solves
Many beginner weather apps either hardcode values or break on duplicate history, poor city matching, timezone confusion, and missing error handling. This application solves that by validating and normalising city names, persisting favourites/history properly, and displaying real weather intelligence from actual API results.

## 4. Technologies Used and Why
- Java Servlet: Handles search, geolocation, favourites, and history actions.
- JSP: Renders the dashboard layout with minimal structural changes.
- JavaScript: Handles live rendering, charts, loading states, dark mode, and user actions.
- SQLite: Default persistent store for favourites and search history.
- MySQL: Optional support only when `WEATHER_DB_URL` is configured; not active by default.
- Open-Meteo APIs: Weather, geocoding, and air-quality data sources.
- Gson: JSON parsing between Java backend and frontend responses.

## 5. Frontend Explanation
The frontend remains a JSP dashboard with cards and sections for current weather, hourly forecast, daily forecast, air quality, alerts, analytics, favourites, theme toggle, and search history. The JavaScript layer updates the dashboard from live JSON responses and ensures the UI reflects actual weather conditions.

## 6. Backend Explanation
The Java backend is organised around the main servlet and a weather client. The servlet validates search terms, calls Open-Meteo endpoints, stores history/favourites in the database, and returns structured JSON to the page. The weather API client normalises geocoding matches, builds forecast arrays, and calculates AQI metadata.

## 7. API Integration Explanation
The app uses Open-Meteo for three main flows:
- geocoding to resolve city names to coordinates
- forecast API for current conditions, hourly data, and 7-day outlook
- air-quality API for AQI, PM2.5, PM10, NO2, ozone, and CO values

The backend validates response payloads before passing results into the dashboard.

## 8. Database Explanation
SQLite is the active persistence layer by default. The app creates and uses `favorites` and `search_history` tables automatically. MySQL is not used unless `WEATHER_DB_URL` is explicitly configured. This is intentionally honest: the project does not pretend to use MySQL when it is not configured.

## 9. System Architecture
The app remains a simple layered design:
- Frontend: JSP + CSS + JavaScript
- Controller: `MyServlet`
- Service layer: `WeatherApiClient`
- Data layer: `WeatherDatabase`

## 10. Data Flow
User enters city -> servlet validates input -> geocoder resolves location -> forecast/AQI APIs return real JSON -> backend normalises values -> frontend renders dashboard -> favourites/history saved in SQLite.

## 11. Challenges Faced
- Ambiguous geocoding results like multiple cities named “Mangalore”
- Duplicate search-history and favourite entries
- Weather field mismatches from real APIs
- Date and timezone interpretation for local forecast displays
- Optional MySQL configuration confusion
- Missing or silent frontend loading/error states

## 12. Solutions Implemented
- Added city normalization before geocoding and storage.
- Deduplicated search history and favourite entries by normalized city name + country.
- Persisted favourites and history through the Java database layer.
- Kept timezone-aware formatting for forecast dates and time labels.
- Added explicit loading and error messages in the UI.
- Rebuilt the Java sources successfully and tested the app through Tomcat.
- Kept the app architecture intact while verifying real API data rather than fake data.

## 13. Actual Interview-Ready Technical Questions and Answers
1. What is this project about? It is a live smart weather dashboard built with Java Servlet and JSP that uses real API data and persistent user state.
2. Why did you choose Java Servlet and JSP? Because the existing project already used that architecture, and it allowed reliable backend + frontend integration without redesigning the app.
3. What external APIs are used? Open-Meteo geocoding, weather forecast, and air-quality APIs.
4. How do you handle city search? The servlet validates the input, resolves the city via the geocoder, and then fetches the weather for the best match.
5. How do you persist favourites? Through the Java data layer into SQLite unless a MySQL URL is configured.
6. How do you persist search history? By inserting unique search entries into `search_history` and preventing repetitions based on normalized city + country values.
7. Why is timezone handling important? Forecast dates and hourly values must be interpreted in the correct local timezone for a user’s location.
8. How do you show forecasts? The API returns hourly and daily time series that the frontend renders into cards and chart labels.
9. How do you handle invalid cities or service failures? The backend returns explicit API-level error messages, and the UI shows status feedback instead of silently failing.
10. What is the active database configuration? SQLite is the active default; MySQL is optional and only used when environment variables are set.
11. How do you validate live data quality? By verifying real API payloads before rendering them in the dashboard and checking the values against actual response fields.
12. What was the main challenge in the app? Maintaining data accuracy while keeping the original architecture and dashboard design intact.

## 14. 10 Questions About Project Improvements
1. Add user authentication and saved profiles
2. Add map view with geolocation overlays
3. Add weather alerts from supported alert sources
4. Add more advanced analytics and trend comparisons
5. Improve dark mode quality and accessibility
6. Add historical climate trends over longer ranges
7. Add multi-language support
8. Add chart export or report sharing
9. Add more mobile-first polish and touch interactions
10. Add deployment automation for Tomcat and CI pipelines

## 15. 2-Minute Interview Summary
This project is a smart weather dashboard implemented with Java Servlets, JSP, JavaScript, and a persistent database layer. It pulls live weather, forecast, geocoding, and air-quality data from Open-Meteo, then renders them in a professional dashboard with favourites, chart analytics, search history, and location-aware behavior. During the audit, I corrected city normalization, duplicate history handling, favourite persistence, timezone-aware date formatting, and UI error/loading states while preserving the architecture. The final state is a verified live app using SQLite by default unless MySQL is explicitly configured.
