# IOT-ABAC-Authentication-Server
Server used for authentication and authorization process of IOT accesss request. Use Attribute Based Access Control policy model.

## Auth Server
Auth Server is written in JAVA Spring Boot and is under package of AuthServer

## OPA Server
OPA Server is written in Go and located at OpaServer package

## Client GUI
Client GUI is written JAVA Swing and under GUIClient

# Getting Started

### Dependencies

* Describe any prerequisites, libraries, OS version, etc., needed before installing program.
* ex. Windows 10

### Installing

* How/where to download your program
* Any modifications needed to be made to files/folders

### Executing program
*For OPA Server:
```
cd OpaServer
go run cmd/server/main.go
```
* For Test Server:
```
cd RemoteTestServer
go run cmd/server/main.go	
```
* For Auth Server:
```
cd AuthServer
Gradle bootrun
```
* For Database Server:
```
cd DBServer
go run cmd/server/main.go
```
* To start IoT UI:
```
java -jar .\out\artifacts\IoTClient_jar\IoTClient_jar.jar 6201
```
* To start Client UI:
```
java -jar .\out\artifacts\UserClient_jar\UserClient_jar.jar
```

## Acknowledgments

Inspiration, code snippets, etc.
* [awesome-readme](https://github.com/matiassingers/awesome-readme)
* [PurpleBooth](https://gist.github.com/PurpleBooth/109311bb0361f32d87a2)
* [dbader](https://github.com/dbader/readme-template)
* [zenorocha](https://gist.github.com/zenorocha/4526327)
* [fvcproductions](https://gist.github.com/fvcproductions/1bfc2d4aecb01a834b46)