#!/bin/bash

# JobPulse Auto Apply - Complete Launcher for macOS
# This script handles everything for you!

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  JobPulse Auto Apply - Complete macOS Launcher       ║${NC}"
echo -e "${BLUE}║  All-in-one startup script                           ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Step 1: Checking Prerequisites...${NC}"
echo ""

missing_tools=()

# Check Docker
if ! command -v docker &> /dev/null; then
    missing_tools+=("Docker")
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    missing_tools+=("Docker Compose")
fi

# Check Java (JAR method)
if ! command -v java &> /dev/null; then
    missing_tools+=("Java 21")
fi

# Check Maven (build)
if ! command -v mvn &> /dev/null; then
    missing_tools+=("Maven")
fi

# Check Git
if ! command -v git &> /dev/null; then
    missing_tools+=("Git")
fi

# Check Node
if ! command -v node &> /dev/null; then
    missing_tools+=("Node.js")
fi

# Check npm
if ! command -v npm &> /dev/null; then
    missing_tools+=("npm")
fi

if [ ${#missing_tools[@]} -gt 0 ]; then
    echo -e "${RED}✗ Missing Prerequisites:${NC}"
    for tool in "${missing_tools[@]}"; do
        echo "  - $tool"
    done
    echo ""
    echo -e "${YELLOW}Please install missing tools first!${NC}"
    echo "Run: ./check-prerequisites.sh"
    echo "Or read: MACOS_DEPLOYMENT_GUIDE.md"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites found!${NC}"
echo ""

# Ask user to choose launch method
echo -e "${BLUE}Step 2: Choose Launch Method${NC}"
echo ""
echo "1) Docker Method (Recommended)"
echo "   - Uses Docker containers for isolation"
echo "   - Requires: Docker Desktop"
echo ""
echo "2) JAR Method (Simple)"
echo "   - Runs JAR directly on your machine"
echo "   - Requires: Java 21, Maven"
echo ""

read -p "Choose (1 or 2): " choice

if [ "$choice" != "1" ] && [ "$choice" != "2" ]; then
    echo -e "${RED}Invalid choice. Exiting.${NC}"
    exit 1
fi

# Clone or update repository
echo ""
echo -e "${BLUE}Step 3: Setting Up Repository${NC}"
echo ""

if [ ! -d ".git" ]; then
    echo "Cloning JobPulse repository..."
    cd ~/Desktop
    git clone https://github.com/vara/JobPulseAutoApply.git 2>/dev/null || \
    git clone https://github.com/your-username/JobPulseAutoApply.git
    cd JobPulseAutoApply
    echo -e "${GREEN}✓ Repository cloned${NC}"
else
    echo "Repository already exists. Updating..."
    git pull origin main 2>/dev/null || echo "Could not pull updates (offline?)"
    echo -e "${GREEN}✓ Repository updated${NC}"
fi

echo ""

# Setup based on choice
if [ "$choice" = "1" ]; then
    echo -e "${BLUE}Step 4: Starting Docker Services${NC}"
    echo ""

    echo "Starting PostgreSQL, Redis, and PgAdmin..."
    docker-compose up -d postgres redis pgadmin

    sleep 3

    echo ""
    if docker-compose ps | grep -q "postgres.*Up"; then
        echo -e "${GREEN}✓ Docker services started${NC}"
        echo ""
        echo "Database credentials:"
        echo "  Host: localhost"
        echo "  Port: 5432"
        echo "  Database: jobpulse"
        echo "  User: jobpulse"
        echo "  Password: jobpulse_password"
        echo ""
        echo "PgAdmin: http://localhost:5050"
        echo "  Email: admin@jobpulse.com"
        echo "  Password: admin"
    else
        echo -e "${RED}✗ Docker services failed to start${NC}"
        echo "Try: docker-compose down && docker-compose up -d"
        exit 1
    fi

    echo ""
    echo -e "${BLUE}Step 5: Building & Starting Backend${NC}"
    echo ""

    echo "Building project (this takes ~2 minutes)..."
    mvn clean install -DskipTests -q

    echo -e "${GREEN}✓ Build complete${NC}"
    echo ""

    echo "Starting Spring Boot application..."
    echo ""
    echo "IMPORTANT: Keep this terminal open"
    echo "Application will be ready at: http://localhost:8080/api/swagger-ui.html"
    echo ""
    echo "In a NEW terminal, run:"
    echo "  cd frontend"
    echo "  npm install"
    echo "  npm run dev"
    echo ""
    echo "Then open browser to: http://localhost:3000"
    echo ""

    mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

else  # JAR method
    echo -e "${BLUE}Step 4: Starting Docker Services (Database only)${NC}"
    echo ""

    echo "Starting PostgreSQL and Redis..."
    docker-compose up -d postgres redis

    sleep 3

    if docker-compose ps | grep -q "postgres.*Up"; then
        echo -e "${GREEN}✓ Database services started${NC}"
    else
        echo -e "${RED}✗ Database failed to start${NC}"
        exit 1
    fi

    echo ""
    echo -e "${BLUE}Step 5: Building Project${NC}"
    echo ""

    echo "Building JAR file (this takes ~2 minutes)..."
    mvn clean install -DskipTests -q

    if [ -f "target/job-pulse-auto-apply-1.0.0.jar" ]; then
        echo -e "${GREEN}✓ JAR file created: target/job-pulse-auto-apply-1.0.0.jar${NC}"
    else
        echo -e "${RED}✗ JAR file not found${NC}"
        exit 1
    fi

    echo ""
    echo -e "${BLUE}Step 6: Starting Backend (JAR)${NC}"
    echo ""

    echo "Starting application..."
    echo ""
    echo "IMPORTANT: Keep this terminal open"
    echo "Application will be ready at: http://localhost:8080/api/swagger-ui.html"
    echo ""
    echo "In a NEW terminal, run:"
    echo "  cd frontend"
    echo "  npm install"
    echo "  npm run dev"
    echo ""
    echo "Then open browser to: http://localhost:3000"
    echo ""

    java -jar target/job-pulse-auto-apply-1.0.0.jar --spring.profiles.active=dev
fi

