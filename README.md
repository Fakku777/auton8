# Auton8 (Prototype) — n8n + MQTT + Minecraft (Fabric + Baritone)

> **Status: Archived / Unmaintained**  
> This repo is an experiment to show a different way to automate Minecraft using **n8n** (no/low-code workflows) talking to a client-side Fabric mod via **MQTT** (Mosquitto). It’s not meant to be production-ready. I’m archiving it to encourage others to **recreate the idea better** and take it to the next level.

## What this is (plain-English)
- The Minecraft **client** runs a Fabric mod (“Auton8”) alongside **Baritone**.
- A local **Mosquitto** MQTT broker moves messages between the mod and **n8n**.
- **n8n** (workflow builder) reacts to in-game events and publishes commands (e.g. `#goto`, `#build`, etc.), so non-Java devs can automate useful sequences.

Think: _“If this happens in game → then do these steps”_ — without writing Java.

## Why this exists
- Baritone can do a lot, but stitching actions together usually means code.
- n8n makes it easy to **chain actions**, add timers/conditions, log to sheets/DBs, etc.
- MQTT gives a simple, robust bridge between the game and your automations.

## How it works (high-level)
1. **Minecraft (Fabric client)**
    - Mods: **Fabric Loader**, **Fabric API**, **Baritone (Fabric)**, and this **Auton8** mod.
    - The mod publishes events to MQTT and subscribes for commands.
2. **Mosquitto (MQTT broker)**
    - Lightweight message bus; persists retained/QoS messages to `mosquitto.db`.
3. **n8n**
    - Workflows subscribe to topics, make decisions, and publish commands back.




---

## Quick Start

### 0) Requirements
- Windows 10/11 (for the provided `.bat` scripts)
- Docker Desktop
- Minecraft + Fabric Loader
- Mods placed in your **`.minecraft/mods`** folder:
    - **Fabric API**
    - **Baritone (Fabric build)**
    - **Auton8** (download the release JAR from this repo)
    - *(If your Baritone build expects a separate Baritone API, include it as well. Some builds bundle it.)*

### 1) Download & install the mod
- Grab the **latest release** from this repo (Auton8 JAR).
- Drop it into your `.minecraft/mods` folder alongside Fabric API and Baritone.

### 2) Start the local services
- Double-click **`start-docker.bat`** (included in the repo).  
  This spins up:
    - **Mosquitto** on `127.0.0.1:1883`
    - **n8n** editor at `http://localhost:5678`

> The first run will auto-create a blank `mosquitto/config/passwd` file and the `mosquitto/data/mosquitto.db` persistence file.

### 3) Run Minecraft
- Launch your Fabric profile and join a world/server.
- The Auton8 mod should connect to MQTT and start sending/receiving topics.

### 4) Build a simple n8n workflow
- Open `http://localhost:5678`
- Create an **MQTT Trigger** → subscribe to your event topic (e.g., `auton8/events/#`)
- Add logic (function/code/IF nodes) → publish to a command topic (e.g., `auton8/cmd`)
- Watch Baritone run your chained steps.

---

## Project layout
