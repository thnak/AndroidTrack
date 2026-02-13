# AndroidTrack

An Android application that connects to an MQTT broker to send device sensor data.

## Features

- **Jetpack Compose UI**: Modern declarative UI framework
- **Android 15 (API 35)**: Requires minimum SDK 35
- **All Device Sensors**: Accesses accelerometer, gyroscope, magnetometer, pressure, light, temperature, and more
- **MQTT Integration**: Publishes sensor data to an MQTT broker in real-time
- **Clean Architecture**: Scalable three-layer architecture (Data, Domain, Presentation)
- **Dependency Injection**: Hilt for clean dependency management
- **Reactive Programming**: Kotlin Coroutines and Flow for asynchronous operations
- **Testable**: Unit tests for ViewModels, UseCases, and Repositories

## Architecture

The project follows Clean Architecture principles with three distinct layers:

### Data Layer
- **Repositories**: `SensorRepository`, `MqttRepository`
- **Models**: `SensorReading`, `MqttConfig`, `MqttConnectionState`
- Handles sensor management and MQTT communication

### Domain Layer
- **Use Cases**: `ObserveSensorDataUseCase`, `PublishSensorDataUseCase`
- **Models**: `SensorData`
- Contains business logic independent of Android framework

### Presentation Layer
- **ViewModels**: `SensorViewModel`
- **UI**: Jetpack Compose screens with Material 3 design
- **Theme**: `AndroidTrackTheme`

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/androidtrack/app/
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   ├── MqttConfig.kt
│   │   │   │   │   ├── MqttConnectionState.kt
│   │   │   │   │   └── SensorReading.kt
│   │   │   │   └── repository/
│   │   │   │       ├── MqttRepository.kt
│   │   │   │       └── SensorRepository.kt
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   └── SensorData.kt
│   │   │   │   └── usecase/
│   │   │   │       ├── ObserveSensorDataUseCase.kt
│   │   │   │       └── PublishSensorDataUseCase.kt
│   │   │   ├── presentation/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   └── Theme.kt
│   │   │   │   │   └── SensorScreen.kt
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── SensorViewModel.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt
│   │   │   └── AndroidTrackApplication.kt
│   │   └── AndroidManifest.xml
│   └── test/
│       └── java/com/androidtrack/app/
│           ├── data/model/
│           │   └── SensorReadingTest.kt
│           ├── domain/usecase/
│           │   └── PublishSensorDataUseCaseTest.kt
│           └── presentation/viewmodel/
│               └── SensorViewModelTest.kt
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Dependency Injection**: Hilt
- **Async**: Kotlin Coroutines & Flow
- **MQTT Client**: Eclipse Paho Android Service
- **Build Tool**: Gradle with Kotlin DSL
- **Testing**: JUnit 4, Mockito, Turbine

## Dependencies

- **Jetpack Compose BOM**: 2024.06.00
- **Hilt**: 2.51.1
- **Eclipse Paho MQTT**: 1.2.5
- **Coroutines**: 1.8.1
- **Testing Libraries**: JUnit, Mockito, Turbine

## Building the Project

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with API 35
- Gradle 8.5

### Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint
```

## Configuration

### MQTT Broker

The default MQTT broker configuration is in `MqttConfig.kt`:

```kotlin
data class MqttConfig(
    val brokerUrl: String = "tcp://broker.hivemq.com:1883",
    val clientId: String = "AndroidTrack_${System.currentTimeMillis()}",
    val username: String? = null,
    val password: String? = null,
    val topic: String = "androidtrack/sensors"
)
```

To use your own MQTT broker, modify these values in the `MqttConfig` class.

## Permissions

The app requires the following permissions:

- `BODY_SENSORS`: Access to health sensors
- `HIGH_SAMPLING_RATE_SENSORS`: High-frequency sensor sampling
- `INTERNET`: Network connectivity for MQTT
- `ACCESS_NETWORK_STATE`: Check network status
- `WAKE_LOCK`: Keep device awake during MQTT operations

## CI/CD Workflows

### Test Workflow (`.github/workflows/test.yml`)
Runs on push and pull requests to `main` and `develop` branches:
- Runs unit tests
- Performs lint checks
- Uploads test and lint reports

### Release Workflow (`.github/workflows/release.yml`)
Runs on version tags (`v*`) or manual trigger:
- Runs tests
- Builds release APK and AAB
- Creates GitHub release with artifacts

## Testing

The project includes unit tests for:
- **Data Models**: Verify data integrity
- **Use Cases**: Test business logic
- **ViewModels**: Validate state management

Run tests:
```bash
./gradlew test
```

## How It Works

1. **Sensor Detection**: On app launch, `SensorRepository` detects all available device sensors
2. **Sensor Monitoring**: Sensors are monitored with `SENSOR_DELAY_NORMAL` sampling rate
3. **Data Flow**: Sensor readings flow through the architecture: Repository → UseCase → ViewModel → UI
4. **MQTT Connection**: App automatically connects to the configured MQTT broker
5. **Data Publishing**: Each sensor reading is published to the MQTT topic as JSON

### Data Format

Sensor data is published as JSON:
```json
{
  "type": "Accelerometer",
  "name": "BMI160 Accelerometer",
  "values": [0.12, 9.81, 0.03],
  "timestamp": 1707789000000
}
```

## Future Enhancements

- [ ] Configurable MQTT broker settings via UI
- [ ] Sensor data buffering and batch publishing
- [ ] Persistent sensor data storage
- [ ] Export sensor data to CSV/JSON files
- [ ] Configurable sensor sampling rates
- [ ] Support for custom sensor filters
- [ ] Dark mode support
- [ ] Instrumented (UI) tests

## License

See the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Support

For issues and feature requests, please use the GitHub issue tracker.
