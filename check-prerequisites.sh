#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   JobPulse Auto Apply - macOS Prerequisites Checker        ║${NC}"
echo -e "${BLUE}║   Date: June 27, 2026                                     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Initialize counters
TOTAL=0
INSTALLED=0
MISSING=0

# Function to check command
check_command() {
    local cmd=$1
    local display_name=$2
    local install_cmd=$3

    echo -n "Checking $display_name... "
    TOTAL=$((TOTAL + 1))

    if command -v "$cmd" &> /dev/null; then
        version=$($cmd --version 2>&1 | head -1)
        echo -e "${GREEN}✓ INSTALLED${NC}"
        echo "   Version: $version"
        INSTALLED=$((INSTALLED + 1))
    else
        echo -e "${RED}✗ NOT INSTALLED${NC}"
        echo "   Install: $install_cmd"
        MISSING=$((MISSING + 1))
    fi
    echo ""
}

# Function to check file/directory
check_path() {
    local path=$1
    local display_name=$2

    echo -n "Checking $display_name... "
    TOTAL=$((TOTAL + 1))

    if [ -d "$path" ] || [ -f "$path" ]; then
        echo -e "${GREEN}✓ FOUND${NC}"
        INSTALLED=$((INSTALLED + 1))
    else
        echo -e "${RED}✗ NOT FOUND${NC}"
        echo "   Location: $path"
        MISSING=$((MISSING + 1))
    fi
    echo ""
}

# Function to check Java version specifically
check_java_version() {
    echo -n "Checking Java Version... "
    if command -v java &> /dev/null; then
        java_version=$(java -version 2>&1 | grep -i "version" | head -1)
        if [[ $java_version == *"21"* ]]; then
            echo -e "${GREEN}✓ JAVA 21${NC}"
            echo "   $java_version"
            INSTALLED=$((INSTALLED + 1))
        else
            echo -e "${YELLOW}⚠ JAVA FOUND BUT NOT 21${NC}"
            echo "   $java_version"
            echo "   Install: brew install openjdk@21"
            MISSING=$((MISSING + 1))
        fi
    else
        echo -e "${RED}✗ NOT INSTALLED${NC}"
        echo "   Install: brew install openjdk@21"
        MISSING=$((MISSING + 1))
    fi
    echo ""
}

# Perform checks
echo -e "${BLUE}=== PACKAGE MANAGERS ===${NC}"
check_command "brew" "Homebrew" "Visit https://brew.sh"
echo ""

echo -e "${BLUE}=== VERSION CONTROL ===${NC}"
check_command "git" "Git" "brew install git"
echo ""

echo -e "${BLUE}=== PROGRAMMING LANGUAGES ===${NC}"
check_java_version
TOTAL=$((TOTAL + 1))
check_command "mvn" "Maven" "brew install maven"
check_command "node" "Node.js" "brew install node"
check_command "npm" "npm" "Included with Node.js"
echo ""

echo -e "${BLUE}=== CONTAINERIZATION ===${NC}"
check_command "docker" "Docker" "Download Docker Desktop: https://www.docker.com/products/docker-desktop"
check_command "docker-compose" "Docker Compose" "Included with Docker Desktop"
echo ""

echo -e "${BLUE}=== DIRECTORIES ===${NC}"
check_path "$HOME/Desktop" "Desktop Folder"
check_path "/Applications/Docker.app" "Docker Desktop App"
echo ""

# Summary
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}SUMMARY${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo "Total checks: $TOTAL"
echo -e "Installed: ${GREEN}$INSTALLED${NC}"
echo -e "Missing: ${RED}$MISSING${NC}"
echo ""

if [ $MISSING -eq 0 ]; then
    echo -e "${GREEN}✓ ALL PREREQUISITES INSTALLED!${NC}"
    echo ""
    echo -e "${BLUE}You can now launch JobPulse Auto Apply!${NC}"
    echo ""
    echo "Choose your launch method:"
    echo "1. Docker Method:  mvn spring-boot:run (+ docker-compose up -d)"
    echo "2. JAR Method:     java -jar target/job-pulse-auto-apply-1.0.0.jar"
    echo ""
    echo "See MACOS_DEPLOYMENT_GUIDE.md for detailed instructions."
else
    echo -e "${YELLOW}⚠ MISSING PREREQUISITES${NC}"
    echo ""
    echo "Install the missing tools using the commands shown above."
    echo "Then run this script again to verify."
    echo ""
    echo -e "${BLUE}Installation Help:${NC}"
    echo "1. Most tools can be installed with: brew install <tool-name>"
    echo "2. Docker Desktop: Download from https://www.docker.com/products/docker-desktop"
    echo "3. For Java 21: brew install openjdk@21"
    echo ""
    echo "See MACOS_DEPLOYMENT_GUIDE.md for complete installation instructions."
fi

echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"

