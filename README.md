# Smart City MQTT Client

A Java implementation of a Publisher and Subscriber for communicating with the Smart City Challenge platform using MQTT protocol with SSL security.

## Project Overview

This project provides a secure MQTT client that connects to the Smart City Challenge platform. It has two main components:

- **Publisher**: Executes system commands (ps, df, ipcs, vmstat) and publishes their output to an MQTT topic
- **Subscriber**: Listens for commands on specific topics and triggers the Publisher to execute them

## Components

- `Publisher.java`: Connects to the MQTT broker and publishes command results
- `Subscriber.java`: Listens to specified topics for incoming commands
- `SubscribeCallback.java`: Handles MQTT callbacks for the Subscriber
- `CmdAccepts.java`: Manages the list of accepted commands
- `Results.java`: Data model for command results 
- `AcceptedCommands.json`: Configuration file defining allowed commands

## Security

The client uses SSL/TLS for secure communication:
- Certificate-based authentication
- TLS v1.2 protocol
- BouncyCastle for cryptographic operations

## Configuration

Certificates are stored in:
- `certificates/`: Client certificates 
- `smartcity-ca/`: CA certificates

## Usage

### Running the Publisher

```bash
java Publisher [broker_url] [command]
```

- If `broker_url` is provided, it will connect to that URL
- If `command` is provided, it will execute that command and publish the result
- If no command is provided, it will cycle through the accepted commands

### Running the Subscriber

```bash
java Subscriber [broker_hostname]
```

- Listens on topics `pissir/all/cmd` and `pissir/20035991/1/cmd`
- When a command is received, executes it and publishes the result

## Accepted Commands

The system only accepts predefined commands for security reasons. The accepted commands are:
- ps
- df
- ipcs
- vmstat

These are defined in `AcceptedCommands.json`.

## Dependencies

- Eclipse Paho MQTT Client
- BouncyCastle for SSL/TLS
- Google Gson for JSON handling
