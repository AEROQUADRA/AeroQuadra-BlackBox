#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <String.h>

// WiFi credentials
const char* ssid = "AqdHub";
const char* password = "12345678";

// WiFi server on port 80
WiFiServer server(80);

// L298N motor driver connections
#define IN1 4  // GPIO4 (D2)
#define IN2 0  // GPIO0 (D3)
#define IN3 16 // GPIO16 (D0)
#define IN4 5  // GPIO5 (D1)
#define ENA 14 // GPIO14 (D5)
#define ENB 2  // GPIO2 (D4)

// Motor speed (0-255)
int motorSpeed = 200;

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

          // Default message when the command does not match any LED
          all_command = command + " is on";

          if (command.equals("FORWARD")) {
            moveForward();
            all_command = "FORWARD move server";
          }

          if (command.equals("BACKWARD")) {
            moveBackward();
            all_command = "BACKWARD move server";
          }

          if (command.equals("LEFT")) {
            rotateLeft();
            all_command = "LEFT move server";
          }

          if (command.equals("RIGHT")) {
            rotateRight();
            all_command = "RIGHT move server";
          }

          if (command.equals("STOP")) {
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

// Function to move the robot forward
void moveForward() {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENA, motorSpeed);  // Set speed
  analogWrite(ENB, motorSpeed);  // Set speed
}

// Function to move the robot backward
void moveBackward() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  analogWrite(ENA, motorSpeed);  // Set speed
  analogWrite(ENB, motorSpeed);  // Set speed
}

// Function to rotate the robot left
void rotateLeft() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENA, motorSpeed);  // Set speed
  analogWrite(ENB, motorSpeed);  // Set speed
}

// Function to rotate the robot right
void rotateRight() {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  analogWrite(ENA, motorSpeed);  // Set speed
  analogWrite(ENB, motorSpeed);  // Set speed
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
