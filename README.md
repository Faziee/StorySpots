
# StorySpots 📍✨

> Share your stories with the world through location-based storytelling

StorySpots is a location-based social media Android application that allows users to share stories, photos, and experiences tied to specific geographic locations. Discover stories from around the world and contribute your own memorable moments to the global map.



## **Authors**

- [@Jordana-GC](https://github.com/Jordana-GC)
- [@Blossom](https://github.com/BlossomAnukposi)
- [@Josephine](https://github.com/XxJosephineStensgaardxX)
- [@Fajar](https://github.com/Faziee)
- [@Sara](https://github.com/SaraBubanova)



## 🌟 Features

### Core Functionality
- **📍 Location-Based Stories**: Share stories pinned to specific geographic coordinates
- **🗺️ Interactive Map**: Explore stories on a beautiful custom Mapbox-powered map
- **📸 Photo Sharing**: Upload and share images with your stories via Cloudinary
- **🔍 Story Discovery**: Discover stories from other users in various locations
- **📱 Modern UI**: Clean, intuitive interface built with Jetpack Compose

### User Features
- **👤 User Authentication**: Secure login and registration with Firebase Auth
- **🔒 Profile Management**: Customizable user profiles with profile pictures
- **📰 Personal Feed**: View and manage your own posted stories
- **🔔 Push Notifications**: Stay updated with interactions on your stories
- **⚙️ Settings**: Comprehensive settings for account management

### Map Features
- **📌 Pin Clustering**: Intelligent clustering of nearby story pins for better map readability
- **🎯 Location Tracking**: Real-time location tracking with permission management
- **🔄 Recenter Button**: Quickly return to your current location
- **💫 Smooth Animations**: Fluid map interactions and transitions



## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with StateFlow and Coroutines
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth
- **Storage**: Cloudinary (image hosting)
- **Maps**: Mapbox SDK
- **Notifications**: Firebase Cloud Messaging (FCM)

### Project Structure
```
com.storyspots/
├── caption/           # Story data models and map loading
├── core/             # Core app components and managers
├── location/         # Location services and management
├── login/            # Authentication screens and logic
├── notificationFeed/ # Notification system
├── pin/              # Map pin clustering and management
├── post/             # Story creation and posting
├── pushNotification/ # FCM implementation
├── register/         # User registration
├── services/         # External service integrations
├── settings/         # App settings and user preferences
├── ui/               # UI components and themes
└── yourFeed/         # Personal story feed
```

### Key Components

#### Core Managers
- **LocationManager**: Handles GPS tracking and location permissions
- **AppComponents**: Centralized dependency injection
- **NavigationManager**: App navigation state management
- **MapStateManager**: Map state and story management

#### Data Layer
- **StoryData**: Core data model for stories
- **NotificationItem**: Model for push notifications
- **CloudinaryService**: Image upload and management



## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24 (Android 7.0) or higher
- Kotlin 1.8+
- Google Services configuration

### Required API Keys
You'll need to set up accounts and obtain API keys for:
1. **Firebase**: For authentication, database, and notifications
2. **Mapbox**: For map functionality
3. **Cloudinary**: For image hosting

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/storyspots.git
   cd storyspots
   ```

2. **Firebase Setup**
   - Create a new project in [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication (Email/Password)
   - Create a Firestore database
   - Enable Cloud Messaging
   - Download `google-services.json` and place it in `app/` directory

3. **Mapbox Configuration**
   - Get your access token from [Mapbox](https://www.mapbox.com/)
   - Add to your `strings.xml`:
   ```xml
   <string name="mapbox_access_token">YOUR_MAPBOX_TOKEN</string>
   ```

4. **Cloudinary Setup**
   - Create account at [Cloudinary](https://cloudinary.com/)
   - Update credentials in `CloudinaryService.kt`:
   ```kotlin
   config["cloud_name"] = "your_cloud_name"
   config["api_key"] = "your_api_key"
   config["api_secret"] = "your_api_secret"
   ```

5. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

### Firebase Database Structure

#### Collections

**users**
```javascript
{
  "userId": {
    "username": "string",
    "email": "string",
    "profileImageUrl": "string",
    "createdAt": "timestamp"
  }
}
```

**story**
```javascript
{
  "storyId": {
    "title": "string",
    "caption": "string",
    "image_url": "string",
    "location": "geopoint",
    "user": "/user/userId",
    "created_at": "timestamp"
  }
}
```

**notification**
```javascript
{
  "notificationId": {
    "title": "string",
    "message": "string",
    "from": "/user/userId",
    "story": "/story/storyId",
    "read": "boolean",
    "created_at": "timestamp"
  }
}
```



## 📱 App Flow

### User Journey
1. **Onboarding**: Login/Register with email and password
2. **Permission Requests**: Location and notification permissions
3. **Map Exploration**: Browse stories on the interactive map
4. **Story Creation**: Share your own stories with photos and location
5. **Social Features**: View notifications and manage your story feed

### Navigation
- **Home**: Interactive map with story pins
- **Your Feed**: Personal stories management
- **Create**: New story creation with camera/gallery
- **Notifications**: Activity feed and interactions
- **Settings**: Profile and app preferences



## 🔧 Key Features Implementation

### Location Services
- **GPS Tracking**: Continuous location updates with battery optimization
- **Permission Handling**: Graceful permission request flow
- **Follow Mode**: Auto-follow user location on map
- **Location Caching**: Persistent location storage

### Story Management
- **Real-time Updates**: Live story feed with Firestore listeners
- **Image Optimization**: Cloudinary integration for efficient image handling
- **Clustering Algorithm**: Smart pin grouping for better UX
- **Offline Caching**: Story data persistence for offline viewing

### Security & Privacy
- **Authentication**: Secure Firebase Auth integration
- **Data Validation**: Client and server-side validation
- **Permission Management**: Granular location and storage permissions
- **User Privacy**: Optional location sharing and story visibility



## 🎨 UI/UX Features

### Design System
- **Material Design 3**: Modern Material You design principles
- **Custom Theme**: Pink-based color scheme with accessibility focus
- **Responsive Layout**: Adaptive UI for different screen sizes
- **Dark Mode**: System-aware theme switching

### Animations
- **Map Transitions**: Smooth camera movements and pin animations
- **Loading States**: Elegant loading indicators and progress bars
- **Micro-interactions**: Subtle feedback for user actions




## 🔔 Notifications

### Push Notifications
- **Story Interactions**: Notifications when users engage with your stories
- **Real-time Delivery**: Firebase Cloud Messaging integration
- **In-App Management**: Notification feed with read/unread states




## 🛠️ Development

### Code Quality
- **MVVM Architecture**: Clear separation of concerns
- **StateFlow**: Reactive state management
- **Coroutines**: Asynchronous programming
- **Type Safety**: Kotlin's null safety and type system

### Testing
- **Unit Tests**: ViewModel and business logic testing
- **UI Tests**: Compose UI testing
- **Integration Tests**: End-to-end user flow testing

### Performance
- **Image Optimization**: Efficient image loading with Coil
- **Memory Management**: Proper lifecycle management
- **Battery Optimization**: Efficient location tracking
- **Network Efficiency**: Smart caching and data fetching




## 📖 API Documentation

### Core Services

#### LocationManager
```kotlin
// Setup location tracking
locationManager.setupLocationComponent(
    mapView = mapView,
    onLocationUpdate = { point -> /* handle update */ },
    centerOnFirstUpdate = true
)

// Get current location
val currentLocation = locationManager.currentLocation
```

#### StoryData Model
```kotlin
data class StoryData(
    val id: String?,
    val title: String?,
    val caption: String?,
    val imageUrl: String?,
    val location: GeoPoint?,
    val createdAt: Timestamp?
)
```



## 🚧 Known Issues & Limitations

- **Offline Mode**: Limited functionality without internet connection
- **Image Size**: Large images may impact app performance
- **Location Accuracy**: GPS accuracy depends on device and environment



## 🔮 Future Enhancements

### Planned Features
- **Story Categories**: Categorize stories by type (food, travel, etc.)
- **Social Features**: Follow users, like/comment on stories
- **AR Integration**: Augmented reality story viewing
- **Story Collections**: Curated story collections and themes
- **Advanced Search**: Filter stories by date, location, category
- **Offline Mode**: Enhanced offline story viewing and creation

### Technical Improvements
- **Modular Architecture**: Break into feature modules
- **Compose Navigation**: Full Compose navigation integration
- **Room Database**: Local caching with Room
- **Dependency Injection**: Dagger/Hilt integration



## 📞 Support

For support, questions or feedback email the developers at their emails bellow.

**Contact**

jordana.guilbride.capela@student.nhlstenden.com
blossom.anukposi1@student.nhlstenden.com
josephine.stensgaard@student.nhlstenden.com
fajar.butt@student.nhlstenden.com
sara.bubanova@student.nhlstenden.com


<div align="center">

**Made with ❤️ for storytellers around the world**

</div>
