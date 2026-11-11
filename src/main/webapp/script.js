const cityInput = document.getElementById('cityInput');
const suggestionList = document.getElementById('citySuggestions');
const searchForm = document.getElementById('searchForm');
const locationBtn = document.getElementById('locationBtn');
const themeToggle = document.getElementById('themeToggle');
const clearHistoryBtn = document.getElementById('clearHistoryBtn');
const favouriteButton = document.getElementById('favouriteBtn');
const statusMessage = document.getElementById('statusMessage');

let hourlyChart = null;
let rainChart = null;
let windChart = null;
let weeklyChart = null;
let currentCityData = null;

function normalizeCityName(city) {
    return (city || '').trim().replace(/\s+/g, ' ').replace(/,$/, '');
}

function setStatus(message, isError, isLoading) {
    statusMessage.textContent = message;
    statusMessage.classList.toggle('is-loading', Boolean(isLoading));
    statusMessage.classList.toggle('is-error', Boolean(isError));
    statusMessage.style.color = isError ? 'var(--danger)' : 'var(--muted)';
}

function setLoadingState(isLoading) {
    const buttons = [searchForm.querySelector('button[type="submit"]'), locationBtn, favouriteButton, clearHistoryBtn, themeToggle];
    buttons.forEach((button) => {
        if (!button) return;
        button.disabled = isLoading;
        button.style.opacity = isLoading ? '0.7' : '1';
    });
}

async function fetchJson(url) {
    let response;
    try {
        response = await fetch(url, { cache: 'no-store' });
    } catch (error) {
        throw new Error('The weather service could not be reached. Please try again in a moment.');
    }

    let data;
    try {
        data = await response.json();
    } catch (error) {
        throw new Error('The weather service returned an invalid response.');
    }

    if (!response.ok || data.success === false) {
        throw new Error(data && data.message ? data.message : 'Request failed.');
    }

    return data;
}

async function loadWeather(city) {
    const normalizedCity = normalizeCityName(city);
    if (!normalizedCity) {
        setStatus('Please enter a valid city name.', true, false);
        return;
    }

    const url = new URL('/WeatherApp/MyServlet', window.location.origin);
    url.searchParams.set('action', 'search');
    url.searchParams.set('city', normalizedCity);

    try {
        setLoadingState(true);
        setStatus(`Fetching live weather for ${normalizedCity}...`, false, true);
        const result = await fetchJson(url.toString());
        renderWeather(result.data);
        cityInput.value = normalizedCity;
        await loadHistory();
    } catch (error) {
        setStatus(error.message, true, false);
    } finally {
        setLoadingState(false);
    }
}

function renderWeatherInsight(data) {
    const container = document.getElementById('weatherInsight');
    if (!container || !data) return;

    const temp = Number(data.temperature || 0);
    const windSpeed = Number(data.windSpeed || 0);
    const humidity = Number(data.humidity || 0);
    const aqi = Number((data.airQuality && data.airQuality.aqi) || 0);
    const dailyForecast = Array.isArray(data.dailyForecast) ? data.dailyForecast : [];
    const firstDaily = dailyForecast[0] || {};
    const rainChance = Number(firstDaily.rainChance || 0);

    const insights = [];

    if (temp >= 30) {
        insights.push('Heat alert: stay hydrated and limit outdoor exposure during peak hours.');
    } else if (temp <= 5) {
        insights.push('Cold alert: wear a warm layer and check for icy conditions outdoors.');
    } else if (rainChance >= 60) {
        insights.push('Rain likely today: carry an umbrella and plan indoor activities.');
    } else {
        insights.push('Comfortable conditions: a great day for outdoor travel and light activity.');
    }

    if (humidity >= 80) {
        insights.push('Humidity is very high. Expect muggy conditions and slower cooling after sunset.');
    } else if (humidity >= 60) {
        insights.push('Humidity is elevated. Light clothing and hydration are a good idea today.');
    } else {
        insights.push('Humidity is manageable, so outdoor comfort should be fairly good.');
    }

    if (rainChance >= 60) {
        insights.push(`Rain probability is ${rainChance}% today, so keep a rain layer handy.`);
    }

    if (windSpeed >= 25) {
        insights.push('Wind is strong, so secure loose items and be careful on open roads.');
    }

    if (aqi >= 151) {
        insights.push('Air quality is poor. Consider reducing long outdoor activity if you are sensitive to pollution.');
    } else if (aqi >= 101) {
        insights.push('Air quality is moderate. Sensitive groups should limit prolonged outdoor exposure.');
    } else if (aqi <= 50) {
        insights.push('Air quality is good. Outdoor conditions look comfortable for most people.');
    }

    if (data.weatherDescription) {
        insights.push(`Current condition: ${data.weatherDescription}.`);
    }

    container.innerHTML = insights.map((item) => `<span class="insight-pill">${item}</span>`).join('');
}

function renderWeather(data) {
    currentCityData = data;

    document.getElementById('cityName').textContent = data.city || 'City';
    document.getElementById('countryName').textContent = data.country || 'Country';
    document.getElementById('weatherIcon').textContent = data.weatherIcon || '☀️';
    document.getElementById('temperatureValue').textContent = Number(data.temperature || 0);
    document.getElementById('weatherDescription').textContent = data.weatherDescription || 'Weather unavailable';
    document.getElementById('feelsLikeText').textContent = `Feels like ${Number(data.feelsLike || 0)}°C`;
    document.getElementById('humidityValue').textContent = `${Number(data.humidity || 0)}%`;
    document.getElementById('windValue').textContent = `${Number(data.windSpeed || 0)} km/h`;
    document.getElementById('pressureValue').textContent = `${Number(data.pressure || 0)} hPa`;
    document.getElementById('visibilityValue').textContent = `${Number(data.visibility || 0)} km`;
    document.getElementById('cloudValue').textContent = `${Number(data.cloudCover || 0)}%`;

    const timezone = data.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone;
    const sunrise = data.sunrise ? formatTime(data.sunrise, timezone) : '--';
    const sunset = data.sunset ? formatTime(data.sunset, timezone) : '--';
    document.getElementById('sunTimesValue').textContent = `${sunrise} / ${sunset}`;

    const dailyForecast = Array.isArray(data.dailyForecast) ? data.dailyForecast : [];
    const firstDaily = dailyForecast[0] || {};
    document.getElementById('minMaxText').textContent = `${firstDaily.minTemp ?? '--'}° / ${firstDaily.maxTemp ?? '--'}°`;

    renderWeatherInsight(data);
    renderHourlyForecast(data.hourlyForecast || [], timezone);
    renderDailyForecast(dailyForecast, timezone);
    renderAirQuality(data.airQuality || {});
    renderAlerts(data.alerts || [], data.alertMessage || 'No alerts available.');
    renderFavouriteButton();
    renderCharts(data.hourlyForecast || [], dailyForecast, timezone);
    setStatus(`Live weather updated for ${normalizeCityName(data.city || 'your selected city')}.`, false, false);
}

function renderHourlyForecast(hourlyForecast, timezone) {
    const container = document.getElementById('hourlyForecast');
    if (!hourlyForecast.length) {
        container.innerHTML = '<div class="empty-state">Hourly forecast data is currently unavailable.</div>';
        return;
    }

    const items = hourlyForecast.slice(0, 8).map((item) => {
        const time = formatHour(item.time, timezone);
        return `
            <div class="forecast-item">
                <div>
                    <div class="time">${time}</div>
                    <div>${item.weatherDescription}</div>
                </div>
                <div class="temp">${item.temperature}°C</div>
            </div>
        `;
    }).join('');
    container.innerHTML = items;
}

function renderDailyForecast(dailyForecast, timezone) {
    const container = document.getElementById('dailyForecast');
    if (!dailyForecast.length) {
        container.innerHTML = '<div class="empty-state">Daily forecast data is currently unavailable.</div>';
        return;
    }

    const items = dailyForecast.map((item) => `
        <div class="daily-item">
            <div class="day">${formatDay(item.date, timezone)}</div>
            <div>${item.weatherIcon || '☀️'} ${item.weatherDescription || 'Weather'}</div>
            <div>${item.minTemp ?? '--'}° / ${item.maxTemp ?? '--'}°</div>
            <div>${item.rainChance ?? 0}% rain</div>
        </div>
    `).join('');
    container.innerHTML = items;
}

function renderAirQuality(quality) {
    const container = document.getElementById('airQualityPanel');
    if (!quality || !quality.aqi && quality.aqi !== 0) {
        container.innerHTML = '<div class="empty-state">Air quality data is not available for this location.</div>';
        return;
    }

    const aqi = Number(quality.aqi || 0);
    const category = quality.category || 'Unknown';
    const status = quality.status || 'Data unavailable.';

    container.innerHTML = `
        <div class="quality-box">
            <span class="badge">AQI ${aqi}</span>
            <strong>${category}</strong>
            <div>${status}</div>
        </div>
        <div class="quality-box">
            <div>PM2.5: ${Number(quality.pm25 || 0)} µg/m³</div>
            <div>PM10: ${Number(quality.pm10 || 0)} µg/m³</div>
            <div>NO₂: ${Number(quality.nitrogenDioxide || 0)} µg/m³</div>
            <div>O₃: ${Number(quality.ozone || 0)} µg/m³</div>
            <div>CO: ${Number(quality.carbonMonoxide || 0)} µg/m³</div>
        </div>
    `;
}

function renderAlerts(alerts, message) {
    const container = document.getElementById('alertPanel');
    if (!alerts || alerts.length === 0) {
        container.innerHTML = `
            <div class="alert-box">
                <strong>System Notice</strong>
                <div>${message}</div>
            </div>
        `;
        return;
    }

    container.innerHTML = alerts.map((alert) => `
        <div class="alert-box">
            <strong>${alert.title || 'Weather Alert'}</strong>
            <div>${alert.description || 'Alert detail not available.'}</div>
        </div>
    `).join('');
}

function renderFavouriteButton() {
    const city = currentCityData && currentCityData.city ? normalizeCityName(currentCityData.city) : '';
    const country = currentCityData && currentCityData.country ? currentCityData.country : '';
    const latitude = currentCityData && currentCityData.latitude ? currentCityData.latitude : 0;
    const longitude = currentCityData && currentCityData.longitude ? currentCityData.longitude : 0;
    favouriteButton.dataset.city = city;
    favouriteButton.dataset.country = country;
    favouriteButton.dataset.latitude = latitude;
    favouriteButton.dataset.longitude = longitude;
    favouriteButton.innerHTML = '<i class="fas fa-star"></i> Save';
}

async function addFavourite(city, country, latitude, longitude) {
    const normalizedCity = normalizeCityName(city);
    if (!normalizedCity) {
        setStatus('Please select a valid city before saving it.', true, false);
        return;
    }

    const url = new URL('/WeatherApp/MyServlet', window.location.origin);
    url.searchParams.set('action', 'addFavourite');
    url.searchParams.set('city', normalizedCity);
    url.searchParams.set('country', country || '');
    url.searchParams.set('latitude', latitude || 0);
    url.searchParams.set('longitude', longitude || 0);

    try {
        setLoadingState(true);
        const result = await fetchJson(url.toString());
        setStatus(`Saved ${normalizedCity} to favourites.`, false, false);
        await loadFavourites();
    } catch (error) {
        setStatus(error.message, true, false);
    } finally {
        setLoadingState(false);
    }
}

async function removeFavourite(city) {
    const normalizedCity = normalizeCityName(city);
    if (!normalizedCity) {
        return;
    }

    const url = new URL('/WeatherApp/MyServlet', window.location.origin);
    url.searchParams.set('action', 'removeFavourite');
    url.searchParams.set('city', normalizedCity);
    try {
        const result = await fetchJson(url.toString());
        setStatus(`${normalizedCity} removed from favourites.`, false, false);
        await loadFavourites();
    } catch (error) {
        setStatus(error.message, true, false);
    }
}

async function loadFavourites() {
    const url = new URL('/WeatherApp/MyServlet', window.location.origin);
    url.searchParams.set('action', 'favourites');

    try {
        const result = await fetchJson(url.toString());
        const favourites = result.favourites || [];
        const container = document.getElementById('favouriteList');
        if (!favourites.length) {
            container.innerHTML = '<div class="empty-state">No favourite cities saved yet.</div>';
            return;
        }

        container.innerHTML = favourites.map((item) => {
            const cityName = normalizeCityName(item.city || 'Unknown city');
            return `
                <div class="favorite-item">
                    <div>
                        <div class="city-name">${cityName}</div>
                        <div>${item.country || ''}</div>
                    </div>
                    <div class="city-temp">${Number(item.temperature || 0)}°C</div>
                    <button type="button" data-city="${cityName}">Remove</button>
                </div>
            `;
        }).join('');

        container.querySelectorAll('button').forEach((button) => {
            button.addEventListener('click', () => removeFavourite(button.dataset.city));
        });
    } catch (error) {
        setStatus(error.message, true, false);
    }
}

async function loadHistory() {
    const url = new URL('/WeatherApp/MyServlet', window.location.origin);
    url.searchParams.set('action', 'history');

    try {
        const result = await fetchJson(url.toString());
        const history = result.history || [];
        const container = document.getElementById('historyList');
        if (!history.length) {
            container.innerHTML = '<div class="empty-state">No recent searches yet.</div>';
            return;
        }

        container.innerHTML = history.map((entry) => {
            const label = normalizeCityName(entry.label || entry.city || 'Unknown');
            const city = normalizeCityName(entry.city || label || 'Unknown');
            return `
                <div class="history-item" data-city="${city}">
                    <div class="city-name">${label}</div>
                    <button type="button" data-city="${city}">Open</button>
                </div>
            `;
        }).join('');

        container.querySelectorAll('.history-item button').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.stopPropagation();
                const city = normalizeCityName(button.dataset.city || button.closest('.history-item')?.dataset.city || '');
                if (city && city.toLowerCase() !== 'current location') {
                    loadWeather(city);
                }
            });
        });
    } catch (error) {
        setStatus(error.message, true, false);
    }
}

async function clearHistory() {
    const url = new URL('/WeatherApp/MyServlet', window.location.origin);
    url.searchParams.set('action', 'clearHistory');

    try {
        await fetchJson(url.toString());
        setStatus('Search history has been cleared.', false, false);
        await loadHistory();
    } catch (error) {
        setStatus(error.message, true, false);
    }
}

async function fetchSuggestions() {
    const query = normalizeCityName(cityInput.value);
    if (!query || query.length < 2) {
        suggestionList.innerHTML = '';
        return;
    }

    try {
        const url = new URL('/WeatherApp/MyServlet', window.location.origin);
        url.searchParams.set('action', 'suggestions');
        url.searchParams.set('city', query);
        const result = await fetchJson(url.toString());
        const suggestions = result.suggestions || [];
        suggestionList.innerHTML = suggestions.map((item) => `
            <option value="${normalizeCityName(item.name)}, ${normalizeCityName(item.country || '')}"></option>
        `).join('');
    } catch (error) {
        // Ignore suggestions errors silently.
    }
}

function formatTime(value, timezone) {
    if (!value) return '--';
    const cleanValue = value.includes('T') ? value.split('T')[1] : value;
    if (!timezone || !cleanValue) return cleanValue ? cleanValue.slice(0, 5) : '--';
    const datePart = value.includes('T') ? value.split('T')[0] : value;
    const timePart = cleanValue.length >= 5 ? cleanValue.slice(0, 5) : cleanValue;
    const zoned = new Date(`${datePart}T${timePart}:00`);
    if (Number.isNaN(zoned.getTime())) {
        return timePart || '--';
    }
    return new Intl.DateTimeFormat(undefined, {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: timezone
    }).format(zoned);
}

function formatHour(value, timezone) {
    if (!value) return '--';
    const parsed = value.includes('T') ? new Date(value) : new Date(`${value}T12:00:00`);
    if (Number.isNaN(parsed.getTime())) {
        return value.includes('T') ? value.split('T')[1].slice(0, 5) : value;
    }
    if (timezone) {
        return new Intl.DateTimeFormat(undefined, {
            hour: '2-digit',
            minute: '2-digit',
            timeZone: timezone
        }).format(parsed);
    }
    return parsed.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatDay(value, timezone) {
    if (!value) return 'Day';
    const parsed = new Date(`${value}T12:00:00`);
    if (Number.isNaN(parsed.getTime())) {
        return value;
    }
    if (timezone) {
        return new Intl.DateTimeFormat(undefined, {
            weekday: 'short',
            timeZone: timezone
        }).format(parsed);
    }
    return parsed.toLocaleDateString([], { weekday: 'short' });
}

function createChart(chartId, type, labels, datasets, options) {
    const canvas = document.getElementById(chartId);
    if (!canvas) return null;

    if (chartId === 'hourlyChart' && hourlyChart) { hourlyChart.destroy(); }
    if (chartId === 'rainChart' && rainChart) { rainChart.destroy(); }
    if (chartId === 'windChart' && windChart) { windChart.destroy(); }
    if (chartId === 'weeklyChart' && weeklyChart) { weeklyChart.destroy(); }

    const chart = new Chart(canvas, {
        type: type,
        data: { labels, datasets },
        options: options || {}
    });

    if (chartId === 'hourlyChart') hourlyChart = chart;
    if (chartId === 'rainChart') rainChart = chart;
    if (chartId === 'windChart') windChart = chart;
    if (chartId === 'weeklyChart') weeklyChart = chart;
    return chart;
}

function renderCharts(hourlyForecast, dailyForecast, timezone) {
    if (!hourlyForecast.length) return;

    const hourlyLabels = hourlyForecast.slice(0, 10).map((item) => formatHour(item.time, timezone));
    const hourlyData = hourlyForecast.slice(0, 10).map((item) => Number(item.temperature || 0));
    const rainData = hourlyForecast.slice(0, 10).map((item) => Number(item.rainChance || 0));
    const windData = hourlyForecast.slice(0, 10).map((item) => Number(item.windSpeed || 0));

    const dailyLabels = dailyForecast.map((item) => formatDay(item.date, timezone));
    const weeklyMax = dailyForecast.map((item) => Number(item.maxTemp || 0));
    const weeklyMin = dailyForecast.map((item) => Number(item.minTemp || 0));

    createChart('hourlyChart', 'line', hourlyLabels, [{
        label: 'Temperature (°C)',
        data: hourlyData,
        borderColor: '#3f8efc',
        backgroundColor: 'rgba(63,142,252,0.2)',
        fill: true,
        tension: 0.36
    }], {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } }
    });

    createChart('rainChart', 'bar', hourlyLabels, [{
        label: 'Rain chance (%)',
        data: rainData,
        backgroundColor: '#4ecdc4'
    }], {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } }
    });

    createChart('windChart', 'radar', hourlyLabels, [{
        label: 'Wind speed',
        data: windData,
        backgroundColor: 'rgba(102, 153, 255, 0.22)',
        borderColor: '#6f7cff'
    }], {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } }
    });

    createChart('weeklyChart', 'line', dailyLabels, [{
        label: 'Max °C',
        data: weeklyMax,
        borderColor: '#ff8f5d',
        backgroundColor: 'rgba(255,143,93,0.18)',
        tension: 0.28,
        fill: false
    }, {
        label: 'Min °C',
        data: weeklyMin,
        borderColor: '#4da3ff',
        backgroundColor: 'rgba(77,163,255,0.18)',
        tension: 0.28,
        fill: false
    }], {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: true } }
    });
}

searchForm.addEventListener('submit', function (event) {
    event.preventDefault();
    const value = normalizeCityName(cityInput.value);
    if (!value) {
        setStatus('Please enter a city name.', true, false);
        return;
    }
    loadWeather(value);
});

cityInput.addEventListener('input', fetchSuggestions);

favouriteButton.addEventListener('click', () => {
    if (!currentCityData) {
        setStatus('Weather data is not loaded yet.', true, false);
        return;
    }
    const city = normalizeCityName(currentCityData.city || '');
    addFavourite(city, currentCityData.country || '', currentCityData.latitude || 0, currentCityData.longitude || 0);
});

clearHistoryBtn.addEventListener('click', clearHistory);

themeToggle.addEventListener('click', () => {
    document.body.classList.toggle('dark-theme');
    const isDark = document.body.classList.contains('dark-theme');
    themeToggle.innerHTML = isDark ? '<i class="fas fa-sun"></i> Light' : '<i class="fas fa-moon"></i> Dark';
    localStorage.setItem('weather-theme', isDark ? 'dark' : 'light');
});

locationBtn.addEventListener('click', () => {
    if (!navigator.geolocation) {
        setStatus('Browser geolocation is not supported on this device.', true, false);
        return;
    }

    setStatus('Requesting your location...', false, true);
    setLoadingState(true);
    navigator.geolocation.getCurrentPosition(async function (position) {
        const url = new URL('/WeatherApp/MyServlet', window.location.origin);
        url.searchParams.set('action', 'geo');
        url.searchParams.set('latitude', position.coords.latitude);
        url.searchParams.set('longitude', position.coords.longitude);

        try {
            const result = await fetchJson(url.toString());
            renderWeather(result.data);
            cityInput.value = normalizeCityName(result.data.city || '');
            setStatus(`Live weather updated for ${normalizeCityName(result.data.city || 'your selected city')}.`, false, false);
        } catch (error) {
            setStatus(error.message, true, false);
        } finally {
            setLoadingState(false);
        }
    }, function () {
        setStatus('Location permission was denied. Please search for a city manually.', true, false);
        setLoadingState(false);
    });
});

window.addEventListener('DOMContentLoaded', async function () {
    const savedTheme = localStorage.getItem('weather-theme');
    if (savedTheme === 'dark') {
        document.body.classList.add('dark-theme');
        themeToggle.innerHTML = '<i class="fas fa-sun"></i> Light';
    }

    await loadWeather('London');
    await loadFavourites();
    await loadHistory();
});
