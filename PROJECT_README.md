# Walkthrough - Antigravity Project (Tasko)

We have successfully scaffolded and implemented the `antigravity-project`, consisting of a Spring WebFlux backend and a Next.js frontend (Tasko Dashboard).

## Project Structure
- `antigravity-project/backend`: Spring Boot application connected to MongoDB.
- `antigravity-project/frontend`: Next.js application with Tailwind CSS.

## Prerequisites
- **Java 21**
- **Maven** (or use `./mvnw` provided in backend)
- **Node.js** & **npm**
- **MongoDB** running locally on port `27017`

## How to Run

### 1. Start the Backend
The backend is configured to run with the `local` profile, which disables GCP Secret Manager and connects to local MongoDB.

```bash
cd antigravity-project/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
*Note: If `./mvnw` fails, verify you have Maven installed and run `mvn spring-boot:run ...` instead.*

**Verify Backend:**
- Server starts on `http://localhost:8080`.
- API Endpoint: `http://localhost:8080/api/projects` returns JSON list of projects.
- Swagger UI: `http://localhost:8080/swagger-ui.html`.

### 2. Start the Frontend
The frontend is configured to proxy API requests or fetch directly from localhost:8080.

```bash
cd antigravity-project/frontend
npm run dev
```

**Verify Frontend:**
- Open `http://localhost:3000` in your browser.
- You should see the **Tasko Dashboard** with:
  - Sidebar navigation.
  - Project stats (Total, Ended, Running).
  - Project Analytics chart.
  - Reminders widgets.

## Features Implemented
- **Backend**:
  - `Project` model and repository.
  - REST API for fetching projects.
  - Data Seeding on startup (creates dummy projects if DB is empty).
  - CORS configuration to allow frontend access.
- **Frontend**:
  - Cleaned up Supabase dependencies.
  - Implemented Sidebar, StatCard, Chart, and Reminder components matching the design.
  - Integrated API fetching from backend.

## Troubleshooting
- **CORS Errors**: Ensure backend is running and `WebConfig.java` allows `localhost:3000`.
- **Database Errors**: Ensure MongoDB is running locally. Check `application-local.yml` configuration.
