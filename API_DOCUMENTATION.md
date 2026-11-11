# API Documentation

## Overview
This application uses Open-Meteo public APIs to provide weather and air-quality information.

## Endpoints

### 1. Search Weather by City
URL: `/WeatherApp/MyServlet?action=search&city={cityName}`
Method: GET
Parameters:
- `city` – name of the city to search

Response:
```json
{
  "success": true,
  "data": {
    "city": "London",
    "country": "United Kingdom",
    "temperature": 10,
    "humidity": 81,
    "windSpeed": 16,
    "weatherDescription": "Clear sky",
    "hourlyForecast": [],
    "dailyForecast": [],
    "airQuality": { "aqi": 73 },
    "alerts": []
  }
}
```

### 2. Fetch Auto-Suggestions
URL: `/WeatherApp/MyServlet?action=suggestions&city={query}`
Method: GET
Returns a list of matching cities with country and coordinates.

### 3. Fetch Weather by Browser Location
URL: `/WeatherApp/MyServlet?action=geo&latitude={lat}&longitude={lon}`
Method: GET
Returns live weather for the device location.

### 4. Get Favourite Cities
URL: `/WeatherApp/MyServlet?action=favourites`
Method: GET
Returns saved favourite cities and current weather values for each.

### 5. Add Favourite City
URL: `/WeatherApp/MyServlet?action=addFavourite&city={city}&country={country}&latitude={lat}&longitude={lon}`
Method: GET or POST

### 6. Remove Favourite City
URL: `/WeatherApp/MyServlet?action=removeFavourite&city={city}`
Method: GET or POST

### 7. Get Search History
URL: `/WeatherApp/MyServlet?action=history`
Method: GET

### 8. Clear Search History
URL: `/WeatherApp/MyServlet?action=clearHistory`
Method: GET or POST

## Error Handling
If a city is invalid or an API request fails, the server returns:
```json
{
  "success": false,
  "message": "No matching city found for: xyz"
}
```

## Data Sources
- Weather and geocoding: Open-Meteo Forecast API
- Air quality: Open-Meteo Air Quality API

## Notes
- The app does not expose any secret keys on the frontend.
- The system clearly indicates when severe weather alert data is unavailable.
