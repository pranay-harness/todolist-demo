Basic `todolist` app.
1. Run `mvn clean install` to generate target/todolist.war
2. Build docker image as: `docker build .`
3. Run docker a: `docker run -p 8080:8080 <image_id>`
4. Simple change for test.
5. This is a test repository.
6. Supports in-memory H2 database with no external setup required.
7. Uses JSP pages with Bootstrap CSS for a responsive frontend interface.
8. Authentication is handled via servlet filters with session-based user tracking.
9. Configured with Maven Compiler Plugin targeting Java 1.6 source compatibility.
10. A Java servlet-based todolist web application that runs on Tomcat with an embedded H2 database.
11. Database connection settings are read from the environment (or from system properties of the same name), never from the source: `TODOLIST_DB_URL`, `TODOLIST_DB_USER`, `TODOLIST_DB_PASSWORD`. All three are optional and default to the embedded in-memory H2 database, so `mvn clean install` and `docker run -p 8080:8080 <image_id>` still work with no setup. When `TODOLIST_DB_URL` points at any other database, `TODOLIST_DB_PASSWORD` must be supplied (e.g. `docker run -e TODOLIST_DB_URL=... -e TODOLIST_DB_USER=... -e TODOLIST_DB_PASSWORD=... ...`) or startup fails; do not commit the value.
12. Last updated: 2026-03-27.
13. Updated README.md with today's date (2026-03-27).
5. abcd
