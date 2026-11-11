# Smart Weather Forecast and Intelligence System

## Overview
Smart Weather Forecast and Intelligence System is a professional Java web application built on the existing WeatherApp project. It provides live weather data, city search, current location support, favourites, search history, analytics, and air-quality monitoring using real external APIs.

## Problem Statement
Traditional weather apps often provide only basic information and lack a complete dashboard for analysis, comparison and user convenience. This project upgrades the app into a real-world weather platform with a strong user experience, proper persistence, and production-style architecture.

## Objectives
- Display real-time weather details from genuine APIs.
- Support city search and browser geolocation.
- Show 7-day and hourly forecast data.
- Monitor air quality from a real AQI source.
- Allow users to save favourite cities and track search history.
- Present the app in a modern dashboard arrangement.
- Build a project that can be demonstrated during interviews and final-year review.

## Technologies Used
- Java Servlet API
- JavaServer Pages (JSP)
- HTML5, CSS3, JavaScript
- Google Chart.js for analytics
- SQLite for local persistence
- Gson for JSON parsing
- Open-Meteo APIs for weather and geocoding
- Open-Meteo Air Quality API
- Apache Tomcat 10

## Architecture
The application follows a simple three-layer design:
1. Frontend: JSP + HTML + CSS + JavaScript rendering dashboard views and UI interactions.
2. Backend: Java Servlet controller handling requests and orchestrating API calls.
3. Data layer: SQLite database for favourites and search history.

## Core Features
- Live current weather information
- Feels-like, humidity, pressure, wind and visibility values
- Sunrise and sunset details
- Hourly and 7-day weather forecast
- City autocomplete suggestions
- Geolocation-based weather lookup
- Favourite city management
- Search history persistence
- Air quality monitoring
- Weather alert notice when provider data is unavailable
- Professional responsive dashboard
- Dark-light theme toggle

## Project Structure
- `src/main/java/MyPackage/MyServlet.java` – main HTTP controller
- `src/main/java/MyPackage/WeatherApiClient.java` – weather and AQI API integration
- `src/main/java/MyPackage/WeatherDatabase.java` – SQLite persistence layer
- `src/main/webapp/index.jsp` – main dashboard UI
- `src/main/webapp/style.css` – dashboard styling
- `src/main/webapp/script.js` – frontend logic and chart rendering
- `src/main/webapp/WEB-INF/web.xml` – servlet mapping configuration

## Setup and Run
1. Install Java JDK 17+.
2. Install Apache Tomcat 10.
3. Copy the project to the Tomcat `webapps` folder or deploy it as a dynamic web project.
4. Ensure that the required JAR files exist in `src/main/webapp/WEB-INF/lib`:
   - `gson-2.8.5.jar`
   - `sqlite-jdbc-3.46.1.0.jar`
   - `slf4j-api-2.0.13.jar`
5. Start Tomcat.
6. Open: `http://localhost:8080/WeatherApp/`

## API Sources
- Open-Meteo Geocoding API
- Open-Meteo Forecast API
- Open-Meteo Air Quality API

## Notes
- This project uses live public APIs and does not rely on fake or hardcoded weather data.
- Weather alerts are shown only when the selected data source supports them; otherwise an explicit system notice is displayed.

## Screenshots
Add screenshots of the dashboard here after running the app locally.

## Developed by
Shilpa Patil © 2026

