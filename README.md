# ThePOC

A modern Android application showcasing various Google Maps and Places API features, built with Jetpack Compose and Material 3 design principles.

## Features

### 🗺️ Maps Integration
- Interactive Google Maps view
- Custom markers for points of interest
- Geofencing capabilities
- Location tracking and updates

### 🔍 Smart Search
- Real-time place search with suggestions
- Address autocomplete using Google Places API
- Smooth animations and transitions
- Search history management

### 📱 Modern UI
- Built with Jetpack Compose
- Material 3 design components
- Dark/Light theme support
- Responsive layout

### ⏱️ Timeline
- Track and display location history
- Visual timeline representation
- Detailed location information

## Technical Stack

### 📚 Libraries & Technologies
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3
- **Maps & Location**: 
  - Google Maps SDK
  - Places API
  - Geofencing API
  - Location Services
- **Database**: Room for local storage
- **Architecture**: MVVM pattern
- **Dependency Injection**: Hilt
- **Build System**: Gradle with version management
- **CI/CD**: Firebase App Distribution

### 🏗️ Architecture
The application follows clean architecture principles with MVVM pattern:
- **UI Layer**: Compose UI components
- **ViewModel Layer**: Business logic and state management
- **Repository Layer**: Data management and API interactions
- **Database Layer**: Local storage with Room

## Development Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Google Maps API key
- Firebase project configuration

### Configuration
1. Clone the repository
```bash
git clone https://github.com/ghostwan/ThePOC.git
```

2. Create a `local.properties` file in the root directory and add your Google Maps API key:
```properties
MAPS_API_KEY=your_api_key_here
```

3. Build and run the project using Android Studio or Gradle:
```bash
./gradlew assembleDebug
```

### Deployment
The project includes an automated deployment script that:
- Increments version numbers
- Builds release APK
- Deploys to Firebase App Distribution

To deploy a new version:
```bash
./tools/deploy.sh
```

## Screenshots
[Add screenshots here]

<!-- Suggested screenshots:
1. Main map view with markers
2. Search interface with suggestions
3. Timeline view
4. Settings/configuration screen
-->

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
[Add your license information here]

## Contact
[Add your contact information here]

---
*Note: This is a proof of concept application demonstrating various Android development best practices and Google Maps API integration.* 