# PartyTime Server - Deployment Guide

This guide explains how to build and deploy the PartyTime Server using containers (Docker or Podman).

## What This Setup Does

This project includes a complete containerized deployment setup:

1. **Dockerfile** - Builds your Spring Boot application into a container image
2. **docker-compose.yaml** - Orchestrates both your app and PostgreSQL database
3. **Makefile** - Provides easy commands to build, run, and manage containers

## Prerequisites

You need either **Docker** or **Podman** installed:

### For Docker:
```bash
# macOS (using Homebrew)
brew install docker docker-compose

# Or install Docker Desktop from https://www.docker.com/products/docker-desktop
```

### For Podman (recommended for macOS):
```bash
# macOS (using Homebrew)
brew install podman podman-compose

# Initialize podman machine
podman machine init
podman machine start
```

## Quick Start

The Makefile automatically detects whether you're using Docker or Podman.

### 1. Build and Start Everything
```bash
make up
```

This will:
- Build your Spring Boot application into a container
- Start PostgreSQL 16 with pgvector extension
- Start your application
- Connect them together

### 2. View Logs
```bash
# All logs
make logs

# Just application logs
make logs-app

# Just database logs
make logs-db
```

### 3. Check Status
```bash
make ps
```

### 4. Access Your Application
- Application: http://localhost:8000
- Database: localhost:5432 (username: PartyTime, password: PartyTime, database: PartyTime)

### 5. Stop Everything
```bash
make down
```

## Common Commands

```bash
make help              # Show all available commands
make build             # Build the application image
make up                # Start all services
make down              # Stop all services
make restart           # Restart all services
make logs              # View logs
make ps                # Show running containers
make clean             # Remove everything including data
make rebuild           # Rebuild and restart
make shell-app         # Open shell in application container
make shell-db          # Open PostgreSQL shell
make package           # Save image as .tar file for deployment
```

## Understanding the Setup

### Multi-Stage Build (Dockerfile)

The Dockerfile uses a **multi-stage build**:

1. **Builder Stage**: Uses Gradle to compile your Java code
   - Caches dependencies for faster rebuilds
   - Builds the Spring Boot JAR

2. **Runtime Stage**: Creates a minimal image with just the JRE
   - Uses Azul Zulu JDK 25 (matches your Nix environment)
   - Runs as non-root user for security
   - Optimized JVM settings for containers

### Service Orchestration (docker-compose.yaml)

The compose file defines two services:

1. **postgres**: PostgreSQL 16 with pgvector extension
   - Stores data in a persistent volume
   - Has health checks to ensure it's ready before the app starts

2. **app**: Your Spring Boot application
   - Waits for database to be healthy before starting
   - Configured via environment variables
   - Automatically restarts if it crashes

### Development vs Production

**Development** (what you're doing now):
- Use your Nix environment (`nix-shell`)
- Local PostgreSQL via `pg_shell` script
- Direct Gradle builds with `./gradlew`

**Production/Deployment** (containers):
- Use `make up` to start everything
- Isolated environment, consistent across machines
- Easy to deploy to servers or cloud platforms

## Deployment Scenarios

### Scenario 1: Deploy to a Linux Server

1. Copy your code to the server
2. Install Docker or Podman on the server
3. Run `make up`
4. Set up a reverse proxy (nginx) to handle HTTPS

### Scenario 2: Save Image and Deploy Elsewhere

```bash
# On your machine: build and package
make package

# This creates: partytime-server-latest.tar

# Transfer to another machine and load it
make load
make up
```

### Scenario 3: Push to a Container Registry

```bash
# Build image
make build

# Tag for your registry
podman tag partytime-server:latest registry.example.com/partytime-server:latest

# Push to registry
podman push registry.example.com/partytime-server:latest
```

## Configuration

Environment variables are set in `docker-compose.yaml`. To override them:

1. Create a `.env` file (not tracked by git)
2. Add variables like:
   ```
   POSTGRES_PASSWORD=my-secure-password
   SERVER_PORT=8080
   ```

## Troubleshooting

### Podman on macOS
If containers can't communicate:
```bash
podman machine stop
podman machine start
```

### Port Already in Use
If port 8000 or 5432 is already in use, edit `docker-compose.yaml` to change the port mapping:
```yaml
ports:
  - "8080:8000"  # Use 8080 instead of 8000
```

### Database Connection Issues
Check that the database is healthy:
```bash
make shell-db
# If this works, database is running
```

### Application Won't Start
Check logs:
```bash
make logs-app
```

## Next Steps for Learning

1. Try modifying code and running `make rebuild` to see changes
2. Experiment with different PostgreSQL configurations
3. Learn about container registries (Docker Hub, GitHub Container Registry)
4. Explore Kubernetes for more advanced orchestration
5. Set up CI/CD pipelines to automatically build and deploy

## Files Created

- `Dockerfile` - How to build your application image
- `docker-compose.yaml` - How to run your full stack
- `.dockerignore` - What to exclude from the build
- `Makefile` - Convenient commands for common tasks
- `DEPLOYMENT.md` - This file
