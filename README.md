
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
   port: 1234 -- the port used to send commands to the application controller via HTTP
   servlet:
      context-path: /simulators -- the base path for the application controller

simulators:
  files:
     path: '${TZC_SIM_DATA_PATH:../some/valid/directory}' -- (or 'C:\some\valid\directory') where on the filesystem can the application find its input file(s)?
     name: '${TZC_SIM_DATA_FILE:SomeValidFilename.csv}' -- the name of the file that the application should load and transmit to simulators, found either at `files.path` or on the classpath; leave this blank if you want to use the file that was packaged with the jar
  clients: [ -- a list of strings in the format `'$HOSTNAME:$PORT'` where `$HOSTNAME` == the IP address or host of a downstream simulator and `$PORT` == the port on which the downstream simulator is listening for TCP traffic
     '${TZC_SIM_URL_0:localhost:1234}', -- if no environment or command line variable is set for `$TZC_SIM_URL_0` then the default `'localhost:1234'` will be used instead
     '${TZC_SIM_URL_1:localhost:5678}' -- add or remove lines as needed to publish simulated TZC transactions to additional downstream simulators
  ]
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
   
It is currently configured to startup, load the data from the configured input file, and immediately transmit
to all configured downstream simulators.  For example, using the default configuration found in `application.yml`:
1. Try to read file named `VehicleSimulationData.csv` from `../src/main/resources` on the filesystem.
2. Try to publish all records from the input file to two downstream simulators, both running on `localhost` and listening for TCP traffic on ports `1234` and `5678` respectively
3. Shutdown

## 4. Other ways to configure

* Using command line arguments:
  -- note that the `clients` element is a list and must have the index of each element specified if configured this way
 ```shell
   java -jar tzc-simulator-coordinator-1.0.jar --simulators.files.path=C:\\dev\\tmp --simulators.files.name=VehicleSimulationData.csv --simulators.clients[0]=localhost:9999 --simulators.clients[1]=localhost:8888
  ```

* Using a different `application.yml` file: place the file in the same directory as the jar   

* Using system/environment variables:
   1. Configure a variable for the desired config parameter, e.g.:
      `export TZC_SIM_DATA_PATH=/some/valid/directory; java -jar tzc-simulator-coordinator-1.0.jar` -- the app will look in `/some/valid/directory` for input files
      `export TZC_SIM_DATA_FILE=SomeOtherFileName.csv; java -jar tzc-simulator-coordinator-1.0.jar` -- the app load the file named `SomeOtherFileName.csv`
   2. Alternatively you can set the variable names immediately before the java command:
      `TZC_SIM_DATA_PATH=/some/valid/directory TZC_SIM_DATA_FILE=SomeOtherFileName.csv TZC_SIM_URL_0=localhost:9999 TZC_SIM_URL_1=localhost:8888 java -jar tzc-coordinator-1.0.jar`


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
