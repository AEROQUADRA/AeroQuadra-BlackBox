
# PathFinder

PathFinder is an advanced navigation application that leverages computer vision to detect ArUco markers and seamlessly control a robot's movement and orientation. This Android-based application processes real-time video feeds to identify ArUco markers, accurately calculate their distance, and then transmit precise movement commands to the robot. The robot's onboard computer, powered by an ESP8266 microcontroller, interprets these commands, ensuring smooth and responsive navigation.

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Hardware Requirements](#hardware-requirements)
- [Software Requirements](#software-requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Application Structure](#application-structure)
- [Command Interface](#command-interface)
- [Contributing](#contributing)
- [Contact](#contact)

## Introduction
PathFinder is a navigation application that uses computer vision to detect ArUco markers and control a robot's movement and orientation. The app processes the video feed to identify markers, calculate the distance to them, and send appropriate commands to the robot via an ESP8266 microcontroller.

## Features
- Detect ArUco markers using OpenCV
- Calculate distance to markers and navigate to their position
- Orient the robot based on the detected marker's ID
- Control robot movement and orientation via ESP8266
- User interface to manually send commands to the robot

## Hardware Requirements
- ESP8266 microcontroller
- L298N motor driver
- DC motors and wheels
- Power supply for the motor driver and ESP8266
- Robot chassis

## Software Requirements
- Android Studio
- OpenCV for Android
- Arduino IDE (for programming the ESP8266)

## Installation

### Android Application
1. Clone the repository:
   \`\`\`sh
   git clone https://github.com/yourusername/PathFinder.git
   cd PathFinder
   \`\`\`
2. Open the project in Android Studio.
3. Build the project to download dependencies and compile the app.
4. Install the app on your Android device.

### ESP8266 Firmware
1. Open the \`esp8266_firmware\` folder in Arduino IDE.
2. Configure the WiFi credentials and pin definitions as per your setup.
3. Upload the code to the ESP8266.

## Usage
1. Power on the ESP8266 and ensure it connects to the specified WiFi network.
2. Open the PathFinder app on your Android device.
3. Calibrate the camera using the "Calibrate Camera" option.
4. Detect ArUco markers using the "Detect ArUco" option.
5. The app will navigate and orient the robot based on the detected marker.
6. Use the "Commands" section to manually control the robot if needed.

## Application Structure
The application consists of the following key activities:

- **MainActivity**: The entry point of the app, providing options to navigate to other activities.
- **DetectArucoActivity**: Captures the camera feed, detects ArUco markers, and initiates navigation.
- **MoveActivity**: Calculates and executes the movement duration based on distance to the marker.
- **RotationActivity**: Adjusts the robot's orientation based on the detected marker's ID.
- **SettingsActivity**: Allows the user to set the wheel RPM and other configurations.
- **CommandsActivity**: Provides a manual control interface for the robot.

### ESP8266 Commands
The ESP8266 listens for HTTP requests and executes the following commands:
- \`FORWARD\`
- \`BACKWARD\`
- \`LEFT\`
- \`RIGHT\`
- \`STOP\`

The commands are sent from the Android app to control the robot's movement and orientation.

### Main Variables
- **Wheel RPM**: The speed of the wheels in rotations per minute (RPM). Set this in the \`SettingsActivity\`.
- **Wheel Diameter**: The diameter of the robot's wheels, used to calculate movement duration.

## Contributing
Contributions are welcome! If you find any issues or have suggestions for improvements, please open an issue or submit a pull request.

## Contact
For issues or questions related to the project, contact udarasampathx@gmail.com.
