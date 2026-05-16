# Quoting Application

Spring Boot application for battery quoting with a Vue/Vite frontend.

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java JDK | 21+ | See install options below |
| Metals Pricing API | — | Must be running before launch |
| Quoting JAR | — | Built with Gradle (see below) |

---

## 1. Install Java 21

If you don't already have Java 21 installed, download it from one of these sources:

- **Microsoft Build of OpenJDK** — https://aka.ms/download-jdk
- **Eclipse Temurin (Adoptium)** — https://adoptium.net
- **Oracle JDK 21** — https://www.oracle.com/java/technologies/downloads/

Install to a directory such as `C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot`.

---

## 2. Build the Application JAR *(skip if you already have the JAR)*

> **Only needed if a pre-built `quoting-*.jar` has not been provided.**  
> If a JAR file is already present next to `start.ps1`, skip straight to [Step 3](#3-start-the-metals-pricing-api).

From the project root, run:

```powershell
./gradlew bootJar
```

This compiles the Java code, builds the Vue UI, and produces a single executable JAR in `build/libs/`.

Copy (or move) the resulting JAR next to `start.ps1`:

```
quoting/
  start.ps1
  quoting-<version>.jar      <-- place it here
  metals_pricing_api.py
```

---

## 3. Start the Metals Pricing API

The quoting application depends on the Metals Pricing mock API. Start it in a **separate terminal** before running `start.ps1`:

```bash
uv run --with 'fastapi[standard]' -- fastapi dev metals_pricing_api.py --host localhost --port 8888
```

> **Tip:** Install `uv` from https://github.com/astral-sh/uv if you don't have it.  
> The API will be available at http://localhost:8888. You can verify it with:
> ```bash
> curl "http://localhost:8888/price?metal=nickel"
> ```

---

## 4. Run the Application

Open a **PowerShell** terminal in the same directory as `start.ps1` and run:

```powershell
.\start.ps1
```

### What the script does

| Step | Action |
|---|---|
| 1 | Checks `JAVA_HOME`, then `PATH`, for a Java 21+ installation |
| 2 | Probes `http://localhost:8888/price?metal=nickel` to confirm the Metals Pricing API is up |
| 3 | Locates the newest `quoting-*.jar` in the same directory (excludes `-plain.jar`) |
| 4 | Waits for the Spring Boot app to start, then opens `http://localhost:8080/battery-quote-center/` in the default browser |
| 5 | Runs the JAR — all application logs are printed to the terminal |

Press **Ctrl+C** to stop the application.

### If Java 21 is not found automatically

The script will prompt you to enter your JDK root directory:

```
  JDK root directory: C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot
```

Enter the **JDK root folder** (not the `bin` subfolder). The script appends `\bin\java.exe` automatically. Type `quit` to exit without launching.

### If the Metals Pricing API is not running

The script exits immediately with:

```
  [FAIL] Metals Pricing API is not running on port 8888.

  Start it in a separate terminal with:

    uv run --with 'fastapi[standard]' -- fastapi dev metals_pricing_api.py --host localhost --port 8888

  Then re-run this script.
```

### Passing extra arguments to the JAR

Any arguments after `.\start.ps1` are forwarded directly to Spring Boot:

```powershell
# Override the server port
.\start.ps1 --server.port=9090

# Activate a Spring profile
.\start.ps1 --spring.profiles.active=prod
```

---

## Application URLs

| URL | Description |
|---|---|
| http://localhost:8080/battery-quote-center/ | Main UI |
| http://localhost:8080/battery-quote-center/api/info | Build info (name, version, build time) |
| http://localhost:8080/battery-quote-center/api/quotes/metals-pricing/health | Metals Pricing API health check |
| http://localhost:8080/battery-quote-center/h2-console | H2 in-memory database console — use JDBC URL `jdbc:h2:mem:quotingdb`, leave password blank |
| http://localhost:8080/battery-quote-center/swagger-ui/index.html | OpenAPI / Swagger UI |
| http://localhost:8888/docs | Metals Pricing API documentation |

---


