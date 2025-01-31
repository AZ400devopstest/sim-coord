TODO: 
1. Refactor/improve input file handling 
2. Fix scheduling -- parsing a 'timeDelay' a simulated TZC transaction record isn't great; we should discuss use cases   
   * Define and configure an additional `TaskScheduler` Bean?
3. Better session management -- in order to support different 'types' of simulator, e.g. PIXI, IDRIS, etc
   * Could handle this via definition of sub-protocols -- one for each 'type' of simulator 
4. Storage?  Seeding in-memory DB at startup?
5. Dockerize -- add some external data storage, replace in-memory DB with something else?
6. async and scheduled exception management -- implement exception handlers

USAGE:
Build and run the jar, then connect to it with a websocket client.
It is currently configured to broadcast all queued messages every minute to all connected clients.
Messages are not currently stored between executions of the jar, and the in-memory data structure is a queue on purpose.
It can be easily refilled by calling the `POST` endpoint.

To find out how many messages are currently queued:
`curl -X GET localhost:4242/simulator/api/coordinator`

To read and queue messages from a file (e.g. VehicleSimulationData.csv):
`curl -X POST localhost:4242/simulator/api/coordinator/loadFromFile/VehicleSimulationData.csv`
* note that the application will look for the file in the location defined in application.yml

To broadcast all queued messages immediately:
`curl -X PUT localhost:4242/simulator/api/coordinator`

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
