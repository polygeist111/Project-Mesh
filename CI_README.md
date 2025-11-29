# Mesh CI

---

## Pipeline Flow

The CI pipeline behaves as follows:

1. run Prettier on web-related filetypes, as well as XML
   a. Do not use prettier-plugin-kotlin, it is not maintained and errors regularly
2. On push, run super-linter. For all possible cases, run autofix (this covers Kotlin)

- Needs updating

---

## To Do

### Sprint 5

- For all workflows, add current build summary as commit/pr comments
- For coverage reporting and complex linting output, add to custom output directory with gitignore (so it can be linked from commit/pr comments, but never pulls to local)
  - to commit to ignored directory, use --force
  - to generate reports in spite of failing tests, run a conditional to check if test tasks failed. If yes, download Kover CLI from https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kover-cli/1.0.715/kover-cli-1.0.715.jar and generate html report from binary
- Allow coverage reports to generate even if tests fail

### Backlog

- Integrate with Dokka
- Test auto UML diagramming
