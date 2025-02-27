# Getting base image
FROM maven:3.9.5-amazoncorretto-21

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

# Define the command to run tzc-simulator-coordinator application with the config file
CMD ["java", "-jar", "tzc-simulator-coordinator-1.0.jar", "--spring.config.location=../src/main/resources/", "--spring.config.name=k8s-config", "run"]
