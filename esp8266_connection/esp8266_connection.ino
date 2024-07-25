#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <String.h>

// WiFi credentials
const char* ssid = "AqdHub";
const char* password = "12345678";

// WiFi server on port 80
WiFiServer server(80);

// TB6612FNG motor driver connections
#define ENA 5 
#define ENB 14  

#define IN1 2  
#define IN2 16

#define IN3 0  
#define IN4 4  

// Motor speed (0-255)
int leftMotorSpeed = 100;
int rightMotorSpeed = 100;

void setup() {
  // Initialize serial communication
  Serial.begin(115200);
  WiFi.softAP(ssid, password); 
  server.begin();
  Serial.println("Access Point started");
  Serial.print("IP address: ");
  Serial.println(WiFi.softAPIP());

  // Initialize L298N pins
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(ENB, OUTPUT);

  // Set initial state of motors to off
  stopMotors();
}

void loop() {
  String all_command = "";
  WiFiClient client = server.available();

  if (client) {
    String request = "";
    while (client.connected()) {
      if (client.available()) {
        char c = client.read();
        request += c;
        if (c == '\r') {
          Serial.println(request);

          int start = request.indexOf("GET /") + 5;
          int end = request.indexOf("HTTP/");
          String command = request.substring(start, end);

          command.replace("\n", "");
          command.replace("\r", "");
          command.replace(" ", ""); 
          command.replace("\t", ""); 
          command.trim();

          Serial.println(command);

          // Parse motor speeds
          parseMotorSpeeds(command);

          // Default message when the command does not match any LED
          all_command = command + " is on";

          if (command.startsWith("FORWARD")) {
            moveForward();
            all_command = "FORWARD move server";
          }

          if (command.startsWith("BACKWARD")) {
            moveBackward();
            all_command = "BACKWARD move server";
          }

          if (command.startsWith("LEFT")) {
            rotateLeft();
            all_command = "LEFT move server";
          }

          if (command.startsWith("RIGHT")) {
            rotateRight();
            all_command = "RIGHT move server";
          }

          if (command.startsWith("STOP")) {
            forceStop();
            all_command = "STOP move server";
          }

          if (client.peek() == '\n') {
            client.println("HTTP/1.1 200 OK");
            client.println("Content-type:text/html");
            client.println();
            String commandWithTags = "<html><body>" + all_command + "</body></html>";
            client.println(commandWithTags);
            break;
          }
        }
      }
    }
  }
}

// Function to parse motor speeds from command
void parseMotorSpeeds(String command) {
  int leftSpeedIndex = command.indexOf("leftSpeed=") + 10;
  int rightSpeedIndex = command.indexOf("rightSpeed=") + 11;

  if (leftSpeedIndex > 9 && rightSpeedIndex > 10) {
    int ampersandIndex = command.indexOf("&", leftSpeedIndex);
    leftMotorSpeed = command.substring(leftSpeedIndex, ampersandIndex).toInt();
    rightMotorSpeed = command.substring(rightSpeedIndex).toInt();
  }
}

// Function to move the robot forward
void moveForward() {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENA, leftMotorSpeed);  // Set left motor speed
  analogWrite(ENB, rightMotorSpeed);  // Set right motor speed
}

// Function to move the robot backward
void moveBackward() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  analogWrite(ENA, leftMotorSpeed);  // Set left motor speed
  analogWrite(ENB, rightMotorSpeed);  // Set right motor speed
}

// Function to rotate the robot left
void rotateLeft() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENA, leftMotorSpeed);  // Set left motor speed
  analogWrite(ENB, rightMotorSpeed);  // Set right motor speed
}

// Function to rotate the robot right
void rotateRight() {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  analogWrite(ENA, leftMotorSpeed);  // Set left motor speed
  analogWrite(ENB, rightMotorSpeed);  // Set right motor speed
}

// Function to stop the robot
void stopMotors() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
  analogWrite(ENA, 0);  // Stop
  analogWrite(ENB, 0);  // Stop
}

// Function to apply a force stop
void forceStop() {
  // Short pulses to quickly stop
  moveForward();
  delay(100);
  moveBackward();
  delay(100);
  moveForward();
  delay(100);
  moveBackward();
  delay(100);
  stopMotors();
}
