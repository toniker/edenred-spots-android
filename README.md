# EdenredSpots 🗺️

A fast and simple Android app to quickly find places where Edenred cards are accepted.

## Overview

EdenredSpots provides a streamlined way to locate merchants and establishments that accept Edenred payment cards. The app displays locations on an interactive map, making it easy to find nearby accepting venues.

## Features

- Interactive map showing Edenred-accepting locations
- Search functionality to find specific places
- Dark mode support
- Clean, modern Material Design 3 interface
- Fast and lightweight

## Setup Requirements

⚠️ **Important**: This app requires a `responses.json` file to function properly.

### Getting the responses.json file

1. Visit the official Edenred website
2. Navigate to the merchant locator or store finder section
3. Extract the JSON response data (found in network requests when loading the map)
4. Save this data as `responses.json`

### Installation Steps

1. Clone this repository
2. Place your `responses.json` file in the appropriate assets directory
3. Open the project in Android Studio
4. Build and run the app

## File Structure

```
app/src/main/
└── res/
    └── raw/
        └── responses.json     <- Place your JSON file here
```

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Maps**: Google Maps SDK for Android
- **Theme**: Material Design 3
- **Architecture**: Modern Android development practices

## Requirements

- Android API 31+ (Android 12.0)
- Google Play Services
- Internet connection for map tiles

## Legal Notice

This app is designed to work with publicly available Edenred merchant data. Users are responsible for obtaining the `responses.json` file through legitimate means from official Edenred sources. This project is not affiliated with or endorsed by Edenred.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

