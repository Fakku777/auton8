# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Auton8 is a proof-of-concept Minecraft automation system that bridges Minecraft gameplay with external automation tools through Minescript API. The project enables remote control of Minecraft using n8n workflows that communicate with a Fabric client mod via HTTP-based Minescript API, with automatic Docker container management.

**Status**: Archived/Unmaintained prototype - designed to inspire proper rebuilds rather than production use.

**Target**: Minecraft 1.21.8 with Fabric, specifically designed to work with Baritone pathfinding.

## Essential Development Commands

### Building the Mod
```bash
./gradlew build
```

### Running in Development
```bash
./gradlew runClient
```

### Clean Build
```bash
./gradlew clean build
```

### Docker Services Management
```bash
# NOTE: n8n container is now auto-managed by the Minecraft mod
# These scripts are optional for manual testing

# Start n8n manually (if auto-start disabled in mod)
./run-docker.sh
# or on Windows:
# run-docker.bat

# Stop services manually
./stop-docker.sh
# or on Windows:
# stop-docker.bat
```

### Working with Docker Compose
```bash
# Start services in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop and remove containers
docker-compose down
```

## Architecture Overview

### Core System Design
The project follows a bridge-based architecture where the `Auton8Core` coordinates multiple specialized bridges that handle different aspects of Minecraft automation:

- **MinescriptClient**: HTTP-based communication layer with n8n via Minescript API
- **DockerManager**: Automatic Docker container management for n8n
- **Auton8Core**: Main coordinator that manages bridge lifecycle and command routing
- **Bridge Pattern**: All integrations implement the `Bridge` interface for consistent lifecycle management

### Key Components

#### Bridge System (`src/main/java/com/kilab/auton8/bridges/`)
- **BaritoneBridge**: Advanced pathfinding control with plan execution, retry logic, and state tracking
- **ChatBridge**: Bidirectional chat integration with deduplication and format parsing
- **TelemetryBridge**: Player position, health, and world state monitoring
- **ConnectionBridge**: MQTT connection state management
- **PlayerBridge**: Player action and status monitoring
- **HudBridge**: In-game HUD integration for status display

#### Minescript Communication (`src/main/java/com/kilab/auton8/minescript/`)
- **MinescriptClient**: Handles HTTP API communication, Server-Sent Events, and session tracking
- **MinescriptMessageHandler**: Interface for handling incoming messages from n8n

#### Docker Management (`src/main/java/com/kilab/auton8/docker/`)
- **DockerManager**: Programmatic container lifecycle management from within Minecraft

#### Module System (`src/main/java/com/kilab/auton8/modules/`)
- **MinescriptLinkModule**: Main Meteor client module with runtime configuration UI
- **OpenWebModule**: Web browser integration for external tool access
- **TimelapseModule**: Screenshot and recording functionality

#### Configuration (`src/main/java/com/kilab/auton8/core/`)
- **Config**: Centralized configuration with MQTT credentials and feature toggles
- **Auton8Core**: System lifecycle management and bridge coordination
- **JsonUtils**: Standardized JSON message formatting

### Minescript API Endpoints
- **Command Endpoint**: `cmd` - Receives automation commands
- **Event Endpoint**: `events` - Publishes game events and status
- **HUD Endpoint**: `hud` - HUD-specific updates
- **Baritone State**: `baritone_state` - Detailed pathfinding state

### Session Management
The system uses UUID-based session IDs to prevent stale command processing:
- New session ID generated on each module activation
- All Minescript API messages include session_id for correlation
- HTTP-based connection monitoring with automatic reconnection
- Session start/end events allow n8n workflows to reset state

### Baritone Integration
Complex pathfinding system with advanced features:
- **Plan Execution**: Multi-step command sequences with retry logic
- **State Tracking**: Real-time monitoring of pathfinding progress
- **Goal Detection**: Automatic completion detection with distance thresholds
- **Stuck Detection**: Automatic retry with exponential backoff
- **Command Types**: Support for goto, path, build, selection, wait, and macro commands

### Development Dependencies
- **Java 21**: Required for compilation and runtime
- **Fabric Loader**: 0.16.14
- **Minecraft**: 1.21.8 (strict version requirement)
- **Meteor Client**: Used as the module framework
- **Baritone**: Pathfinding library (requires external JAR files in `libs/`)
- **OkHttp**: HTTP client library for Minescript API communication
- **Docker Java API**: Programmatic Docker container management

## Hard-coded Values (Prototype Limitations)
Current build contains hardcoded Minescript credentials in `MinescriptLinkModule.java`:
- Base URL: `http://127.0.0.1:5679/api`
- Client ID: `kilab-pc1`
- Auth Key: `your-super-strong-auth-key`

## External Dependencies
- **Docker Desktop**: Required for n8n container (auto-managed by mod)
- **Baritone JARs**: Must be placed in `libs/` directory (baritone.jar, nether-pathfinder-1.4.1.jar)
- **n8n**: Workflow automation tool running on localhost:5678
- **Minescript API**: HTTP API running on localhost:5679

## File Structure Context
```
src/main/java/com/kilab/auton8/
├── Auton8.java                 # Main addon entry point
├── bridges/                    # Bridge implementations for different systems
├── core/                       # Core configuration and system management
├── docker/                     # Docker container management
├── hud/                        # In-game HUD components
├── minescript/                 # Minescript API communication layer
└── modules/                    # Meteor client modules with UI
```

The project uses standard Gradle-based Fabric modding conventions with Meteor client integration for the module system.
