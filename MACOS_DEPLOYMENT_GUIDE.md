# 🍎 JobPulse Auto Apply - Complete macOS Deployment Guide

**Platform**: macOS (Intel & Apple Silicon compatible)  
**Date**: June 27, 2026  
**Deployment Methods**: Docker OR Java JAR  
**Time to Full System**: ~10 minutes  

---

## 📋 PRE-REQUISITES CHECKLIST

### What You Need
1. **Docker & Docker Desktop** (for Docker method)
2. **Java 21 JDK** (for JAR method)
3. **Git** (to clone repository)
4. **Node.js & npm** (for frontend)
5. **Homebrew** (optional, for easy installation)

### Quick Check Commands
```bash
# Run these in Terminal to check what you have
docker --version
docker-compose --version
java -version
git --version
node --version
npm --version
brew --version
```

---

## ✅ STEP 1: VERIFY & INSTALL PREREQUISITES

### 1.1 Check if Homebrew is Installed
```bash
brew --version
```

**If NOT installed**: Copy & paste this entire command:
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```
Then run:
```bash
source $HOME/.zprofile
brew --version  # Verify
```

---

### 1.2 Check & Install Git
```bash
git --version
```

**If NOT installed**:
```bash
brew install git
git --version  # Verify
```

---

### 1.3 Check & Install Java 21 (Required for JAR method)
```bash
java -version
```

**If you see Java version** (any version): You have Java installed  
**If NOT installed or need Java 21 specifically**:

```bash
brew install openjdk@21
```

After installation, set JAVA_HOME:
```bash
# Add to your shell profile (~/.zprofile or ~/.bash_profile)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zprofile
source ~/.zprofile

# Verify Java 21
java -version
```

Expected output:
```
openjdk version "21.x.x" 2023-...
```

---

### 1.4 Check & Install Maven (Required for building)
```bash
mvn -version
```

**If NOT installed**:
```bash
brew install maven
mvn -version  # Verify

# Output should show: Apache Maven 3.9.x
```

---

### 1.5 Check & Install Docker Desktop (For Docker method)
```bash
docker --version
docker-compose --version
```

**If NOT installed**:
1. Go to: https://www.docker.com/products/docker-desktop
2. Click "Download for Mac"
3. Choose your chip:
   - **Apple Silicon** (M1, M2, M3): `Docker.dmg` (Apple Silicon)
   - **Intel**: `Docker.dmg` (Intel Chip)
4. Double-click the `.dmg` file
5. Drag Docker icon to Applications folder
6. Open Applications → Docker
7. Follow setup wizard
8. Verify:
```bash
docker --version
docker-compose --version
```

---

### 1.6 Check & Install Node.js & npm (For Frontend)
```bash
node --version
npm --version
```

**If NOT installed**:
```bash
brew install node
node --version  # Should show v18+
npm --version   # Should show 9+
```

---

## 🎯 COMPLETE PREREQUISITE VERIFICATION

Run this script to check everything at once:

```bash
#!/bin/bash
echo "🔍 Checking JobPulse Prerequisites..."
echo ""

echo "✓ Git:"
git --version || echo "❌ NOT INSTALLED"

echo ""
echo "✓ Java 21:"
java -version 2>&1 | head -1 || echo "❌ NOT INSTALLED"

echo ""
echo "✓ Maven:"
mvn -version | head -1 || echo "❌ NOT INSTALLED"

echo ""
echo "✓ Docker:"
docker --version || echo "❌ NOT INSTALLED"

echo ""
echo "✓ Docker Compose:"
docker-compose --version || echo "❌ NOT INSTALLED"

echo ""
echo "✓ Node.js:"
node --version || echo "❌ NOT INSTALLED"

echo ""
echo "✓ npm:"
npm --version || echo "❌ NOT INSTALLED"

echo ""
echo "✓ Homebrew:"
brew --version | head -1 || echo "❌ NOT INSTALLED"

echo ""
echo "Done! Install any marked as NOT INSTALLED"
```

Save as `check-prereqs.sh`:
```bash
chmod +x check-prereqs.sh
./check-prereqs.sh
```

---

## 📊 PREREQUISITES INSTALLATION SUMMARY

| Tool | Check Command | Install Command | Required For |
|------|---------------|-----------------|--------------|
| Homebrew | `brew --version` | Manual (see above) | Package manager |
| Git | `git --version` | `brew install git` | Clone repository |
| Java 21 | `java -version` | `brew install openjdk@21` | JAR method |
| Maven | `mvn -version` | `brew install maven` | Build project |
| Docker | `docker --version` | Docker Desktop DMG | Docker method |
| Docker Compose | `docker-compose --version` | Included with Docker | Docker method |
| Node.js | `node --version` | `brew install node` | Frontend |
| npm | `npm --version` | Included with Node.js | Frontend |

---

## 🚀 OPTION 1: LAUNCH WITH DOCKER (Easiest)

### Prerequisites Check
```bash
docker --version      # Should show Docker version
docker-compose --version  # Should show docker-compose version
git --version         # Should show git version
node --version        # Should show node v18+
npm --version         # Should show npm v9+
```

### Step 1: Clone Repository
```bash
cd ~/Desktop  # or any directory you prefer
git clone https://github.com/vara/JobPulseAutoApply.git
cd JobPulseAutoApply
```

If you already cloned it:
```bash
cd ~/path/to/JobPulseAutoApply
git pull origin main  # Update to latest
```

### Step 2: Start Docker Services (Terminal 1)
```bash
# Start PostgreSQL, Redis, PgAdmin
docker-compose up -d postgres redis pgadmin

# Wait for services to be ready
sleep 10

# Verify services are running
docker-compose ps
```

Expected output:
```
NAME          STATUS
postgres      Up (healthy)
redis         Up
pgadmin       Up
```

### Step 3: Build & Run Backend (Terminal 2)
```bash
# From project root
mvn clean install -DskipTests

# Run Spring Boot application
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Wait for message:
```
Tomcat started on port(s): 8080 (http)
```

### Step 4: Setup & Start Frontend (Terminal 3)
```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev
```

Wait for message:
```
VITE v5.x.x ready in xxx ms
Local: http://localhost:3000
```

### Step 5: Access Application
1. Open Browser: **http://localhost:3000**
2. Register new account
3. Login and start using!

### Step 6: Stop Everything
When done:
```bash
# Terminal 1 (Docker)
docker-compose down

# Terminal 2 (Backend) - Press Ctrl+C
# Terminal 3 (Frontend) - Press Ctrl+C
```

---

## 🍯 OPTION 2: LAUNCH WITH JAR FILE

### Prerequisites Check
```bash
java -version        # Should show Java 21
git --version        # Should show git version
node --version       # Should show node v18+
npm --version        # Should show npm v9+
```

### Step 1: Clone Repository
```bash
cd ~/Desktop
git clone https://github.com/vara/JobPulseAutoApply.git
cd JobPulseAutoApply
```

### Step 2: Start Database with Docker (Terminal 1)
```bash
# Only Docker services needed (database + redis)
docker-compose up -d postgres redis

# Wait for startup
sleep 10

# Verify
docker-compose ps
```

### Step 3: Build JAR File (Terminal 2)
```bash
# From project root
mvn clean install -DskipTests

# Takes ~1-2 minutes
# Creates: target/job-pulse-auto-apply-1.0.0.jar
```

### Step 4: Run JAR File
```bash
java -jar target/job-pulse-auto-apply-1.0.0.jar --spring.profiles.active=dev
```

Wait for:
```
Tomcat started on port(s): 8080 (http)
```

### Step 5: Setup & Start Frontend (Terminal 3)
```bash
cd frontend
npm install
npm run dev
```

### Step 6: Access Application
Open browser: **http://localhost:3000**

### Step 7: Stop Everything
```bash
# Terminal 1 (Docker) - Press Ctrl+C or in new terminal:
docker-compose down

# Terminal 2 (JAR) - Press Ctrl+C
# Terminal 3 (Frontend) - Press Ctrl+C
```

---

## 🔧 METHOD COMPARISON

```
┌──────────────────────┬────────────────────────┬────────────────────────┐
│       FEATURE        │   DOCKER METHOD        │    JAR METHOD          │
├──────────────────────┼────────────────────────┼────────────────────────┤
│ Setup Time           │ ~3 minutes             │ ~5 minutes             │
│ Total Files Downloaded│ Large (Docker images) │ Just JAR (88MB)       │
│ Disk Space Needed    │ 5-10 GB                │ 1-2 GB                 │
│ Isolation            │ Complete (containers)  │ Shared system          │
│ Prerequisites        │ Docker + Node + Git    │ Java + Maven + Node    │
│ Production Ready     │ YES                    │ YES                    │
│ Easy to Update       │ YES (rebuild image)    │ YES (rebuild JAR)      │
│ Multi-Developer      │ BETTER                 │ BETTER                 │
│ Learning Curve       │ Medium                 │ Easy                   │
└──────────────────────┴────────────────────────┴────────────────────────┘
```

**Recommendation**: 
- **Beginners**: Use JAR method (simpler)
- **Teams/Production**: Use Docker method (better isolation)

---

## ✅ VERIFICATION CHECKLIST

### After Launching (Any Method)

**Test Backend Health**:
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2026-06-27T...",
  "database": "UP"
}
```

**Test Frontend**:
```bash
curl http://localhost:3000
```

Should return HTML content (page exists)

**Access in Browser**:
- http://localhost:8080/api/swagger-ui.html (API docs)
- http://localhost:3000 (UI application)

**Database Connection**:
```bash
# Docker terminal
docker-compose exec postgres psql -U jobpulse -d jobpulse -c "SELECT 1;"
```

Expected output:
```
 ?column?
----------
        1
(1 row)
```

---

## 🚨 TROUBLESHOOTING

### "Command not found: docker"
```bash
# Install Docker Desktop first
# Check: docker --version
# If still not working, restart Terminal or Mac
```

### "Command not found: java"
```bash
# Install Java 21
brew install openjdk@21

# Set JAVA_HOME
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zprofile
source ~/.zprofile
```

### "Port 8080 already in use"
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use different port
java -jar app.jar --server.port=8081
```

### "Port 3000 already in use"
```bash
# Find process
lsof -i :3000

# Kill it
kill -9 <PID>

# Or different port
npm run dev -- --port 3001
```

### "Docker daemon is not running"
```bash
# Open Docker Desktop application
# Wait for Docker icon to appear in menu bar
docker ps  # Verify it's running
```

### "Connection refused to localhost:5432"
```bash
# Check if PostgreSQL is running
docker-compose ps

# If not, start it
docker-compose up -d postgres
sleep 10
docker-compose ps
```

### "Maven build fails"
```bash
# Clear Maven cache
mvn clean

# Rebuild
mvn install -DskipTests

# If still failing
mvn clean install -DskipTests -U  # Update dependencies
```

### "npm install hangs"
```bash
# Clear npm cache
npm cache clean --force

# Try again
npm install
```

### "Can't login to application"
```bash
# Clear browser cache
# Open DevTools → Application → Clear all data
# Refresh page
# Try login again
```

### "Application loading but blank page"
```bash
# Check console for errors
# Open DevTools → Console tab
# Check if backend is running
curl http://localhost:8080/api/health
# Check frontend logs in terminal
```

---

## 📊 COMPLETE STARTUP WORKFLOW

```
Step 1: Verify Prerequisites
  └─ java, git, npm, docker (or maven for JAR)
     └─ Missing? Install with Homebrew

Step 2: Clone Repository
  └─ git clone [repo-url]
     └─ cd JobPulseAutoApply

Step 3: Start Database Services
  └─ docker-compose up -d postgres redis
     └─ Wait for healthy status

Step 4: Build & Start Backend
  └─ DOCKER: mvn spring-boot:run
  └─ JAR: mvn clean install && java -jar app.jar

Step 5: Start Frontend
  └─ cd frontend
     └─ npm install (first time only)
     └─ npm run dev

Step 6: Access Application
  └─ http://localhost:3000
     └─ Register → Login → Use!

Step 7: Stop Everything
  └─ Ctrl+C on all terminals
     └─ docker-compose down
```

---

## 🎯 QUICK REFERENCE COMMANDS

### Docker Method
```bash
# Terminal 1: Database
docker-compose up -d postgres redis
docker-compose ps

# Terminal 2: Backend
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Terminal 3: Frontend
cd frontend && npm install && npm run dev

# Cleanup
docker-compose down
```

### JAR Method
```bash
# Terminal 1: Database
docker-compose up -d postgres redis
docker-compose ps

# Terminal 2: Backend
mvn clean install -DskipTests
java -jar target/job-pulse-auto-apply-1.0.0.jar

# Terminal 3: Frontend
cd frontend && npm install && npm run dev

# Cleanup
docker-compose down
```

---

## 📱 SYSTEM REQUIREMENTS

### Minimum
- macOS 10.15 or later
- 4GB RAM
- 2GB disk space
- Intel or Apple Silicon processor

### Recommended
- macOS 12 or later
- 8GB RAM
- 5GB disk space
- Apple Silicon (M1+) or Intel i5+

### Internet
- ~500MB for Docker images (Docker method)
- ~500MB for Maven dependencies
- ~200MB for Node modules

---

## 🔗 USEFUL LINKS

| Tool | Link | macOS Intel/Apple Silicon |
|------|------|---------------------------|
| Homebrew | https://brew.sh | Both ✓ |
| Docker Desktop | https://www.docker.com/products/docker-desktop | Both ✓ |
| Java 21 | https://brew.sh | Both ✓ |
| Git | https://git-scm.com/download/mac | Both ✓ |
| Node.js | https://nodejs.org | Both ✓ |

---

## ✨ FINAL CHECKLIST

Before starting, verify:
- [ ] Homebrew installed: `brew --version`
- [ ] Git installed: `git --version`
- [ ] Java 21 installed: `java -version` (shows 21.x)
- [ ] Maven installed: `mvn -version` (shows 3.9+)
- [ ] Node.js installed: `node --version` (shows 18+)
- [ ] npm installed: `npm --version` (shows 9+)
- [ ] Docker installed: `docker --version` (for Docker method)
- [ ] Docker running: Docker app open and visible in menu bar

All items checked? ✅ You're ready to launch!

---

## 🚀 FINAL COMMAND (Copy & Paste)

### Setup Everything at Once (JAR Method)
```bash
# Copy this entire block and paste in terminal

cd ~/Desktop && \
git clone https://github.com/vara/JobPulseAutoApply.git && \
cd JobPulseAutoApply && \
docker-compose up -d postgres redis && \
sleep 5 && \
mvn clean install -DskipTests && \
echo "✅ Setup complete! Run these in separate terminals:" && \
echo "Terminal 1: mvn spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=dev'" && \
echo "Terminal 2: cd frontend && npm install && npm run dev" && \
echo "Then open: http://localhost:3000"
```

---

## 📞 GETTING HELP

1. **Verify Prerequisites**: Run `./check-prereqs.sh`
2. **Check Logs**: Look for error messages in terminal
3. **Test Health**: `curl http://localhost:8080/api/health`
4. **Check Database**: `docker-compose ps`
5. **Review Documentation**: See `LOCAL_LAUNCH_GUIDE.md` in project

---

**You're all set!** 🎉 Follow one of the two methods above and you'll have a fully functional JobPulse Auto Apply system running on your Mac!

