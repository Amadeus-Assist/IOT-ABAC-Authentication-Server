# IOT-ABAC-Authentication-Server
Server used for authentication and authorization process of IOT accesss request. Use Attribute Based Access Control policy model.

## Auth Server
Auth Server is written in JAVA Spring Boot and is under package of AuthServer

## OPA Server
OPA Server is written in Go and located at OpaServer package

## Client GUI
Client GUI is written JAVA Swing and under GUIClient

## How to run the servers

For OPA Server:
cd OpaServer
go run cmd/server/main.go

For Test Server:

cd RemoteTestServer
go run cmd/server/main.go	

For Auth Server:
cd AuthServer
Gradle bootrun

For Database Server:
cd DBServer
go run cmd/server/main.go

To start IoT UI:
java -jar .\out\artifacts\IoTClient_jar\IoTClient_jar.jar 6201

To start Client UI:
java -jar .\out\artifacts\UserClient_jar\UserClient_jar.jar
