
# **StorySpots**

This README details the usage of the StorySpots application.
How to install it and its purpose.




## **Authors**

- [@Jordana-GC](https://github.com/Jordana-GC)
- [@Blossom](https://github.com/Jordana-GC)
- [@Josephine](https://github.com/Jordana-GC)
- [@Fajar](https://github.com/Jordana-GC)
- [@Sara](https://github.com/Jordana-GC)



## **Overview**

StorySpots is a Kotlin-based Android application that enables users to share and explore stories tied to real-world locations. The app features both public and private map views where users can create and view pinned locations enriched with photos and stories.

**Built With**

Kotlin – Modern Android development language

Google Maps SDK – For rendering and interacting with maps

Jetpack (ViewModel, Navigation, LiveData) – Clean architecture and lifecycle management

FusedLocationProviderClient – Accurate user location detection


## **Key Features**

**Public Map:**
Users can explore a shared map populated with posts from all users. Pins represent places where stories, photos, or experiences have been shared.

**Private Maps:**
Users can create or join private maps that are shared among specific people or groups. Ideal for collaborative storytelling (e.g., travel groups, friends, family).

**Location-based Posting:**
Users can create posts with images and text, pinned to a specific geographic location.

**Map Interaction:**
Users can pan, zoom, and interact with markers on the map.

**User Privacy & Permissions:**
App handles geolocation permissions and gracefully manages scenarios where location access is denied.

## **Prerequisites**

Before you build and run this project, make sure you have the following installed and configured:

-Android Studio Giraffe or later (latest stable recommended)

-JDK 17+

-Gradle 8+ (handled automatically by Android Studio)

-Google Maps API Key

-Internet connection (for loading map tiles and fetching location data)

-An Android device or emulator with:

  - Google Play Services enabled

  - Location Services enabled
## **Usage**

Follow these steps to build and run the application:

**Clone the Repository**


    git clone https://github.com/yourusername/mapstories.git

    cd mapstories

**Open in Android Studio**

Open the project folder in Android Studio.

Let Gradle sync and resolve all dependencies.

**Add your Google Maps API Key**

In local.properties:

    MAPS_API_KEY=your_api_key_here

Or directly in AndroidManifest.xml (for testing only):

    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="your_api_key_here" />

**Run the App**

Select a physical device or emulator.

Click Run (Shift + F10 or the play button).

Grant location permissions when prompted.

**Explore the Map**

The app will request your location.

You can pan/zoom around the map.

## **Support**

For support, questions or feedback email the developers at their emails bellow.

**Contact**

jordana.guilbride.capela@student.nhlstenden.com
blossom.anukposi1@student.nhlstenden.com
josephine.stensgaard@student.nhlstenden.com
fajar.butt@student.nhlstenden.com
sara.bubanova@student.nhlstenden.com