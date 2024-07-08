#include <ESP8266WiFi.h>
#include <WiFiClient.h>

#include <String.h>


#define GREEN LED_BUILTIN


const char* ssid = "Yout_Tube_WIFI_APP";
const char* password = "12345678";
WiFiServer server(80);

void setup() {
  Serial.begin(115200);
  WiFi.softAP(ssid, password); 
  server.begin();
  Serial.println("Access Point started");
  Serial.print("IP address: ");
  Serial.println(WiFi.softAPIP());  

  pinMode(GREEN, OUTPUT);

  digitalWrite(GREEN, LOW);

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

          if (command.equals("red")) {
            digitalWrite(GREEN, LOW);
            all_command = "Green built-in LED is off, Red and Blue are on";
          }

          if (command.equals("green")) {
            digitalWrite(GREEN, HIGH);
            all_command = "Green built-in LED is off, Red and Blue are on";
          }

          if (command.equals("blue")) {
            all_command = "Nothing";
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
