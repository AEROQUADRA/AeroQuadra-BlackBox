#include <ESP8266WiFi.h>
#include <WiFiClient.h>

#include <String.h>


#define LED LED_BUILTIN


const char* ssid = "AqdHub";
const char* password = "12345678";
WiFiServer server(80);

void setup() {
  Serial.begin(115200);
  WiFi.softAP(ssid, password); 
  server.begin();
  Serial.println("Access Point started");
  Serial.print("IP address: ");
  Serial.println(WiFi.softAPIP());  

  pinMode(LED, OUTPUT);

  digitalWrite(LED, LOW);

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
            digitalWrite(LED, HIGH);
            all_command = "FORWARD move server";
          }

          if (command.equals("BACKWARD")) {
            digitalWrite(LED, HIGH);
            all_command = "BACKWARD move server";
          }

          if (command.equals("LEFT")) {
            digitalWrite(LED, HIGH);
            all_command = "LEFT move server";
          }

          if (command.equals("RIGHT")) {
            digitalWrite(LED, HIGH);
            all_command = "RIGHT move server";
          }

          if (command.equals("STOP")) {
            digitalWrite(LED, LOW);
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
