# Smart Campus

## Run locally (no database needed: uses in-memory H2)
    cd backend
    mvn spring-boot:run
Copy frontend/index.html, style.css, app.js into backend/src/main/resources/static/ and open http://localhost:8080

## Cloud database (PostgreSQL, e.g. Neon)
    DB_URL=jdbc:postgresql://HOST/DBNAME?sslmode=require
    DB_USER=...   DB_PASSWORD=...
Tables are created automatically from schema.sql.

## Deploy
Backend: Render (Docker, root directory = backend). Frontend: Vercel (root directory = frontend, edit vercel.json with your Render URL).
