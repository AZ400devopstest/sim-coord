
TODO: 
1. Refactor/improve input file handling 
2. Fix scheduling -- parsing a 'timeDelay' a simulated TZC transaction record isn't great; we should discuss use cases   
   * Define and configure an additional `TaskScheduler` Bean?
3. Better session management -- in order to support different 'types' of simulator, e.g. PIXI, IDRIS, etc
   * Could handle this via definition of sub-protocols -- one for each 'type' of simulator 
4. Storage?  Seeding in-memory DB at startup?
5. Dockerize -- add some external data storage, replace in-memory DB with something else?
6. async and scheduled exception management -- implement exception handlers

# USAGE:

## 1. Modify `src/main/resources/application.yml` as needed:

```yaml
server:
   port: 1234 -- the port used to send commands to the application via HTTP
   servlet:
      context-path: /simulators -- the base path for the application controller

simulators:
    pixi:
      autostart: 'true/false' -- should the application immediately load a file and start transmitting to simulators?
      files:
        path: '/some/valid/filesystem/directory/' (or 'C:\some\valid\directory\'  -- where on the filesystem can the application find its input file(s)?
        name: 'SomeValidFileName.csv' -- the name of the file that the application should load and transmit to simulators
    server:
      host: 'localhost' -- the hostname of the pixi simulator which should receive data
      port: 5678 -- the port on which the pixi simulator is listening for TCP traffic
```
   
## 2. Build the runnable jar (or start the application via run config in your IDE): `mvn clean package`

```shell
mvn clean package
[INFO] --- jar:3.4.2:jar (default-jar) @ tzc-simulator-coordinator ---
[INFO] Building jar: C:\dev\repos\simulator-coordinator\target\tzc-simulator-coordinator-1.0.jar
[INFO] 
[INFO] --- spring-boot:3.4.2:repackage (repackage) @ tzc-simulator-coordinator ---
[INFO] Replacing main artifact C:\dev\repos\simulator-coordinator\target\tzc-simulator-coordinator-1.0.jar with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] The original artifact has been renamed to C:\dev\repos\simulator-coordinator\target\tzc-simulator-coordinator-1.0.jar.original
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.037 s
[INFO] Finished at: 2025-01-31T10:58:09-08:00
[INFO] ------------------------------------------------------------------------
```

## 3. Start the application: `java -jar tzc-simulator-coordinator-1.0.jar`
   
It is currently configured to broadcast all queued messages every minute to all connected clients.
Messages are not currently stored between executions of the jar, and the in-memory data structure is a queue on purpose.
It can be easily refilled by calling the `POST` endpoint.

To find out how many messages are currently queued:
`curl -X GET localhost:4242/simulators/api/coordinator`

To read and queue messages from a file (e.g. VehicleSimulationData.csv):
`curl -X POST localhost:4242/simulators/api/coordinator/loadFromFile/VehicleSimulationData.csv`
* note that the application will look for the file in the location defined in application.yml

To broadcast all queued messages immediately:
`curl -X PUT localhost:4242/simulators/api/coordinator`

To change the filesystem location (directory) where the application checks for input files:
`curl -X PUT localhost:4242/simulators/api/coordinator/newFilePath -d "newFilePath=/some/valid/directory"`

sudo apt install curl zip

Install tool sdkman useful for maintaining java versions:
============================================================
curl -s "https://get.sdkman.io" | bash

source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk list java

sdk install java 21.0.2-open

sdk default java 21.0.2-open

to confirm java installation and its version:
=============================================
java -version

Install maven using sdkman. Maven is build/package tool.
============================================================
sdk list maven

sdk install maven

sdk install maven 3.9.6

mvn -v

sdk default maven <version>

For example:
=============
sdk default maven 3.8.7

to build and package:
======================
cd tzc-coordinator-project

mvn clean package

to execute the coordinator:
=================================================================================

`java -jar target/tzc-coordinator-new-project-1.0.SNAPSHOT.jar`
Application is now listening on configured port
