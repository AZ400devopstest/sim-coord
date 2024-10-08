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

to execute the coordinator: (with default config.properties which is part of jar)
=================================================================================

java -jar target/tzc-coordinator-new-project-1.0.SNAPSHOT.jar

to execute the coordinator using config.properties at custom location
=====================================================================
java -jar target/tzc-coordinator-new-project-1.0.SNAPSHOT.jar <custom location>

for example:
================
java -jar target/tzc-coordinator-new-project-1.0.SNAPSHOT.jar ~/simulator-coordinator/config.properties
