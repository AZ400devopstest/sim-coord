# Getting base image
FROM jelastic/maven:3.9.5-openjdk-21

# Setting work directory
WORKDIR /app

# Copy application code into the image
COPY . .

# Creating the tzc-coordinator application jar
RUN mvn package
 
# Setting work directory
WORKDIR /app/target/

# Expose the required port
EXPOSE 4242

# Coomand to run the tzc-coordinator application
ENTRYPOINT ["java", "-jar", "tzc-simulator-coordinator-1.0.jar"]