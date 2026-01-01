# PartyTime Server - Deployment Guide

This guide explains how to build and deploy the PartyTime Server using Podman.

## Prerequisites

Install Podman and podman-compose:

```bash
# Fedora/RHEL/CentOS
sudo dnf install podman podman-compose

# Ubuntu/Debian
sudo apt install podman
pip install podman-compose

# macOS
brew install podman podman-compose
podman machine init
podman machine start
```

## Quick Start

### Start Everything
```bash
podman-compose up -d
```

This will:
- Build your Spring Boot application
- Start PostgreSQL 16 database
- Start your application
- Connect them together

### View Logs
```bash
podman-compose logs -f
```

### Stop Everything
```bash
podman-compose down
```

## Access Your Application

- Application: http://localhost:8000
- Database: localhost:5432 (username: PartyTime, password: PartyTime, database: PartyTime)

## Common Commands

```bash
# Start services
podman-compose up -d

# View logs
podman-compose logs -f app
podman-compose logs -f postgres

# Stop services
podman-compose down

# Rebuild after code changes
podman-compose up -d --build

# Remove everything including data
podman-compose down -v
```

## Running Just the Database

If you want to develop locally but use a containerized database:

```bash
# Start only PostgreSQL
podman-compose up -d postgres

# Run your app locally
./gradlew bootRun
```

## How It Works

The `Dockerfile` uses a multi-stage build:
1. **Builder stage**: Compiles your Java code with Gradle
2. **Runtime stage**: Creates a minimal image with just the JRE

The `docker-compose.yaml` defines:
1. **postgres**: PostgreSQL 16 with persistent data storage
2. **app**: Your Spring Boot application configured to connect to the database

## Troubleshooting

### View what's running
```bash
podman ps
```

### Check logs if something fails
```bash
podman-compose logs app
podman-compose logs postgres
```

### Rebuild from scratch
```bash
podman-compose down -v
podman-compose up -d --build
```
