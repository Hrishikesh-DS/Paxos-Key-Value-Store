# Project 1 - Instructions

## Prerequisites

- Ensure you have Java Development Kit (JDK) installed.
- Ensure you have Apache Maven installed.

## Step-by-Step Instructions

### 1. Generate Classes

Navigate to the root directory of your project and run the following command to generate the necessary classes:

javac Client/\*.java  
javac Server/\*.java
javac Log/\*.java

This will generate the java classes in the client and the server and Log directory respectively.

### 2. Run the following command

- rmiregistry -J-Djava.class.path=bin &

### 3. Running the Server

1. Navigate and Run the server code:

- java Server/ServerClass

### 4. Running the Client

1. Open a new terminal window.

2. Run the client code:

- java Client/ClientClass

### 5. Executing the client

- Enter the PUT, GET or DELETE Command to execute the queries

- CTRL/Command + C to terminate the server or client

### Notes

- Ensure that this command is run first to initialize the RMI -> rmiregistry -J-Djava.class.path=bin &
- Ensure the server is running before starting the client.
