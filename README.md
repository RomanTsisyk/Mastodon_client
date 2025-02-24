# Mastodon Timeline Viewer

A real-time Mastodon timeline viewer application for Android, providing a seamless experience for viewing and searching public content from the Mastodon social network.

## Features

- Stream public posts from Mastodon in real-time
- Search for specific terms or topics
- View cached content when offline
- Interactive gesture to remove posts from the timeline
- Properly formatted HTML content in posts
- Fluid interface with proper loading and error states

## Architecture

The application is built using Clean Architecture principles with MVVM pattern, organized into three layers:

- Data Layer -> External data sources and persistence
- Domain Layer -> Business logic and use cases
- UI Layer -> User interface and interactions
```
app/
├── src/
│   ├── main/
│   │   ├── java/io/github/romantsisyk/mastodon/
│   │   │   ├── data/
│   │   │   │   ├── api/
│   │   │   │   ├── database/
│   │   │   │   ├── di/
│   │   │   │   ├── manager/
│   │   │   │   ├── preferences/
│   │   │   │   └── repository/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   └── usecase/
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   ├── state/
│   │   │   │   ├── theme/
│   │   │   │   └── views/
│   │   │   └── utils/
│   │   └── res/
│   └── test/
└── build.gradle
```
