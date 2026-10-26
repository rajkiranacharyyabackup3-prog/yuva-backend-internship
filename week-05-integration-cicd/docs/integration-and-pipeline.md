# Week 5 Integration and Pipeline Notes

## Integration checks

`Week5IntegrationTests` sends HTTP requests through the Spring MVC and security layers against an in-memory H2 database. It checks that an unauthenticated write is rejected, an authenticated post is saved and can be read in a later request, a different account cannot edit it, invalid post input returns a structured `400`, and an unknown post returns `404`.

The test uses the same API contract and test profile as the application integration suite. To run the suite from this project directory, use `mvnw.cmd test` on Windows or `./mvnw test` on Linux and macOS.

## Continuous integration workflow

The GitHub Actions workflow is stored in `.github/workflows/week5-integration-cd.yml` in the repository root and mirrored in this project folder so the Week 5 ZIP is self-contained. A push or pull request that changes this folder runs the Maven verification phase with Java 21. The job then uploads the packaged `blog-api.jar` as a workflow artifact.

The second job downloads that same artifact and copies it into a temporary staging directory. This demonstrates a test-to-artifact-to-staging handoff; it does not deploy to a live server, require credentials, or contact an external service.

## Local simulation

Run `powershell -ExecutionPolicy Bypass -File pipeline/simulate-cd.ps1` from the Week 5 project folder. The script runs `clean verify`, confirms that the JAR exists, copies it to `target/cd-simulation`, and checks that the staged file is non-empty. The staging directory is generated output and is excluded from the ZIP package.

The workflow configuration is ready for GitHub Actions. A hosted workflow result requires committing and pushing the files to GitHub; the local simulation does not claim that a remote Actions run has already occurred.
