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
- Add report systems to other workflows

### Backlog

- Integrate with Dokka
- Test auto UML diagramming
- Break workflows out into multiple intelligently-grouped jobs for improved execution visibility
