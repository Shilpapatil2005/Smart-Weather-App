<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Smart Weather Forecast and Intelligence System</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <link rel="stylesheet" href="style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body class="theme-light">
    <div class="page-shell">
        <header class="topbar">
            <div class="brand-block">
                <div class="brand-icon"><i class="fas fa-cloud-sun-rain"></i></div>
                <div>
                    <h1>Smart Weather</h1>
                    <p>Forecast & Intelligence System</p>
                </div>
            </div>
            <div class="topbar-actions">
                <button id="themeToggle" class="ghost-button" type="button"><i class="fas fa-moon"></i> Dark</button>
            </div>
        </header>

        <section class="search-panel">
            <form id="searchForm" class="search-form">
                <div class="input-group">
                    <input id="cityInput" list="citySuggestions" type="text" placeholder="Search city worldwide" autocomplete="off" />
                    <datalist id="citySuggestions"></datalist>
                    <button id="searchBtn" type="submit"><i class="fas fa-search"></i> Search</button>
                    <button id="locationBtn" class="secondary-button" type="button"><i class="fas fa-location-crosshairs"></i> My Location</button>
                </div>
            </form>
            <div id="statusMessage" class="status-message" aria-live="polite">Loading weather intelligence...</div>
        </section>

        <main class="dashboard">
            <section class="hero-card card">
                <div class="hero-header">
                    <div>
                        <p class="eyebrow">Current location</p>
                        <h2 id="cityName">London</h2>
                        <span id="countryName" class="country-label">United Kingdom</span>
                    </div>
                    <button id="favouriteBtn" class="favourite-button" type="button"><i class="fas fa-star"></i> Save</button>
                </div>
                <div class="hero-body">
                    <div id="weatherIcon" class="hero-icon">☀️</div>
                    <div>
                        <div class="hero-temp"><span id="temperatureValue">--</span><span class="unit">°C</span></div>
                        <div id="weatherDescription" class="weather-description">Loading weather...</div>
                    </div>
                    <div class="temp-range">
                        <span id="feelsLikeText">Feels like --°C</span>
                        <span id="minMaxText">--° / --°</span>
                    </div>
                </div>
            </section>

            <section class="insight-panel card">
                <div class="panel-header">
                    <h3>Smart Weather Insight</h3>
                </div>
                <div id="weatherInsight" class="insight-content">
                    <div class="insight-pill">Loading local guidance...</div>
                </div>
            </section>

            <section class="metrics-grid">
                <article class="metric-card card">
                    <div class="metric-icon"><i class="fas fa-droplet"></i></div>
                    <div>
                        <p>Humidity</p>
                        <h3 id="humidityValue">--%</h3>
                    </div>
                </article>
                <article class="metric-card card">
                    <div class="metric-icon"><i class="fas fa-wind"></i></div>
                    <div>
                        <p>Wind</p>
                        <h3 id="windValue">-- km/h</h3>
                    </div>
                </article>
                <article class="metric-card card">
                    <div class="metric-icon"><i class="fas fa-gauge-high"></i></div>
                    <div>
                        <p>Pressure</p>
                        <h3 id="pressureValue">-- hPa</h3>
                    </div>
                </article>
                <article class="metric-card card">
                    <div class="metric-icon"><i class="fas fa-eye"></i></div>
                    <div>
                        <p>Visibility</p>
                        <h3 id="visibilityValue">-- km</h3>
                    </div>
                </article>
                <article class="metric-card card">
                    <div class="metric-icon"><i class="fas fa-cloud"></i></div>
                    <div>
                        <p>Cloud Cover</p>
                        <h3 id="cloudValue">--%</h3>
                    </div>
                </article>
                <article class="metric-card card">
                    <div class="metric-icon"><i class="fas fa-sun"></i></div>
                    <div>
                        <p>Sunrise / Sunset</p>
                        <h3 id="sunTimesValue">-- / --</h3>
                    </div>
                </article>
            </section>

            <section class="split-row">
                <article class="panel card">
                    <div class="panel-header">
                        <h3>Hourly Forecast</h3>
                    </div>
                    <div id="hourlyForecast" class="forecast-list"></div>
                </article>

                <article class="panel card">
                    <div class="panel-header">
                        <h3>7-Day Forecast</h3>
                    </div>
                    <div id="dailyForecast" class="daily-list"></div>
                </article>
            </section>

            <section class="split-row">
                <article class="panel card">
                    <div class="panel-header">
                        <h3>Air Quality</h3>
                    </div>
                    <div id="airQualityPanel" class="quality-panel"></div>
                </article>

                <article class="panel card">
                    <div class="panel-header">
                        <h3>Weather Alerts</h3>
                    </div>
                    <div id="alertPanel" class="alert-panel"></div>
                </article>
            </section>

            <section class="split-row">
                <article class="panel card">
                    <div class="panel-header">
                        <h3>Favourite Cities</h3>
                    </div>
                    <div id="favouriteList" class="list-panel"></div>
                </article>

                <article class="panel card">
                    <div class="panel-header">
                        <h3>Recent Search History</h3>
                        <button id="clearHistoryBtn" class="link-button" type="button">Clear</button>
                    </div>
                    <div id="historyList" class="list-panel"></div>
                </article>
            </section>

            <section class="analytics-panel card">
                <div class="panel-header">
                    <h3>Weather Analytics</h3>
                </div>
                <div class="chart-grid">
                    <div class="chart-box"><canvas id="hourlyChart"></canvas></div>
                    <div class="chart-box"><canvas id="rainChart"></canvas></div>
                    <div class="chart-box"><canvas id="windChart"></canvas></div>
                    <div class="chart-box"><canvas id="weeklyChart"></canvas></div>
                </div>
            </section>
        </main>
    </div>

    <footer class="page-footer">Developed by Shilpa Patil © 2026</footer>

    <script src="script.js"></script>
</body>
</html>
