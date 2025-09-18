# n8n-nodes-minecraft

A comprehensive collection of n8n nodes for Minecraft automation using Baritone, Minescript, and Meteor Client.

## Overview

This package provides powerful n8n nodes that enable you to automate Minecraft gameplay through workflows. It integrates with:

- **Baritone**: Advanced pathfinding, building, and mining automation
- **Minescript**: Python scripting and command execution in Minecraft
- **Meteor Client**: Module control and client-side automation
- **Auton8**: Custom integration bridge (from your existing project)

## Installation

1. Install the package in your n8n instance:
```bash
npm install n8n-nodes-minecraft
```

2. Restart your n8n instance to load the new nodes.

3. Set up your Minecraft environment with the required mods:
   - Minescript mod for Minecraft 1.21.8+ (Fabric)
   - Meteor Client
   - Baritone (integrated with Meteor)

## Quick Start

### 1. Configure Credentials

Create a new credential of type "Minescript API" with:
- **Base URL**: `http://127.0.0.1:5679/api` (default Minescript API)
- **Client ID**: Your unique client identifier
- **Auth Key**: Your authentication key
- **Session ID**: Optional session ID for command correlation

### 2. Basic Workflow Example

```
[Manual Trigger] → [Minecraft Session: Start Session] → [Baritone Pathfinder: Go to Coordinates] → [Minescript Command: Take Screenshot]
```

This workflow starts a session, moves the player to coordinates (100, 64, 100), and takes a screenshot.

## Available Nodes

### Baritone Nodes

#### Baritone Pathfinder
**Purpose**: Control movement and pathfinding
**Operations**:
- Go to Coordinates
- Go to Block Type  
- Follow Player
- Explore Area
- Stop/Cancel
- Set Goal
- Start Pathfinding

**Example**: Navigate to diamond ore
```json
{
  "operation": "gotoBlock",
  "blockType": "diamond_ore"
}
```

#### Baritone Builder
**Purpose**: Construction and building operations
**Operations**:
- Build Schematic
- Clear Area
- Fill Area
- Mine Blocks
- Farm Crops
- Create Tunnels

**Example**: Build a schematic
```json
{
  "operation": "build",
  "schematicFile": "house.schematic",
  "originX": "~",
  "originY": "~", 
  "originZ": "~"
}
```

### Minescript Nodes

#### Minescript Command
**Purpose**: Execute Python scripts and commands
**Operations**:
- Execute Python Script
- Evaluate Python Code
- Send Chat Message
- Echo Message (private)
- Execute Minecraft Command
- Copy/Paste Regions
- Take Screenshots
- Job Management (suspend/resume/kill)

**Example**: Send a chat message
```json
{
  "operation": "chat",
  "message": "Hello from n8n automation!"
}
```

### Meteor Client Nodes

#### Meteor Module
**Purpose**: Control Meteor Client modules
**Operations**:
- Toggle Modules (on/off)
- Configure Settings
- Get Module Status
- List Modules
- Save/Load Profiles

**Example**: Toggle Kill Aura
```json
{
  "operation": "toggle",
  "moduleCategory": "combat",
  "moduleName": "kill-aura"
}
```

### Session Management

#### Minecraft Session Manager
**Purpose**: Handle sessions and events
**Operations**:
- Start/End Sessions
- Listen for Events
- Get Player/World/Inventory Info
- Get Entities
- Get Block Information

**Example**: Listen for chat events
```json
{
  "operation": "listenEvents",
  "eventTypes": ["chat", "movement"],
  "eventTimeout": 30
}
```

## Advanced Usage

### Session Correlation

Use session IDs to correlate commands across multiple nodes:

```
[Start Session] → [Store Session ID] → [Multiple Operations using same Session ID]
```

### Event-Driven Workflows

Set up reactive workflows that respond to game events:

```
[Listen Events] → [Filter Chat Messages] → [Auto-Response] → [Log to Database]
```

### Complex Building Operations

Chain multiple building operations:

```
[Copy Region] → [Move to Location] → [Paste Region] → [Take Screenshot] → [Send Notification]
```

### Automated Mining Workflows

```
[Mine Diamonds] → [Check Inventory] → [If Full] → [Go to Base] → [Store Items] → [Return to Mining]
```

## Configuration Examples

### PvP Automation Setup
```json
{
  "workflow": [
    {
      "node": "MeteorModule", 
      "operation": "toggle",
      "moduleName": "kill-aura"
    },
    {
      "node": "BaritonePathfinder",
      "operation": "goto", 
      "coordinates": [0, 100, 0]
    },
    {
      "node": "MinescriptCommand",
      "operation": "screenshot",
      "screenshotName": "pvp_location"
    }
  ]
}
```

### Base Building Automation
```json
{
  "workflow": [
    {
      "node": "BaritoneBuilder",
      "operation": "build",
      "schematicFile": "base_foundation.schematic"
    },
    {
      "node": "BaritoneBuilder", 
      "operation": "build",
      "schematicFile": "base_walls.schematic"
    },
    {
      "node": "MinescriptCommand",
      "operation": "chat",
      "message": "Base construction completed!"
    }
  ]
}
```

## Error Handling

All nodes support n8n's built-in error handling:

- **Continue on Fail**: Workflow continues even if a node fails
- **Retry Logic**: Configurable retry attempts for failed operations
- **Error Output**: Detailed error information in node outputs

## Troubleshooting

### Common Issues

1. **Connection Errors**
   - Verify Minescript API is running on the correct port
   - Check authentication credentials
   - Ensure Minecraft client is running with required mods

2. **Command Failures**
   - Check Baritone settings (allowBreak, allowPlace, etc.)
   - Verify player has necessary permissions
   - Ensure schematic files exist in correct directory

3. **Session Issues**
   - Use unique session IDs for different workflows
   - End sessions properly to avoid conflicts
   - Check session timeouts in long-running workflows

### Debug Mode

Enable debug logging by setting the `DEBUG` environment variable:
```bash
DEBUG=n8n-nodes-minecraft* npm start
```

## API Reference

### Node Properties

All nodes support these common properties:
- **operation**: The operation to perform
- **advancedOptions**: Collection of advanced settings
- **continueOnFail**: Whether to continue workflow on node failure

### Credential Types

#### MinescriptApi
- `baseUrl`: Minescript API endpoint
- `clientId`: Client identifier  
- `authKey`: Authentication key
- `sessionId`: Optional session ID

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

MIT License - see LICENSE file for details.

## Support

For support and questions:
- GitHub Issues: [Create an issue](https://github.com/kilab/n8n-nodes-minecraft/issues)
- Discord: Join the Auton8 community server
- Documentation: [Full documentation](https://github.com/kilab/n8n-nodes-minecraft/wiki)

## Changelog

### v1.0.0
- Initial release
- Baritone pathfinding and building nodes
- Minescript command execution nodes  
- Meteor Client module control nodes
- Session management and event handling
- Comprehensive error handling and logging

## Related Projects

- [Auton8](https://github.com/kilab/auton8) - The Minecraft automation bridge this package was designed for
- [Baritone](https://github.com/cabaletta/baritone) - Minecraft pathfinding bot
- [Minescript](https://github.com/maxuser0/minescript) - Python scripting for Minecraft
- [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) - Minecraft utility mod
- [n8n](https://n8n.io) - Workflow automation platform
