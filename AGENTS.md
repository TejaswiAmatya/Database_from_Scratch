# Repository Guidelines

## Project Structure & Module Organization
Source lives under `src/main/java/com/tejaswi/database`, tests under `src/test/java/...` in a mirrored package structure. Two packages exist today:
- `index` — the B+ tree implementation. `BPlusTreeNode` is the abstract base (holds `maxKeys` and `keys` as package-private/protected fields, no getters); `LeafNode` and `InternalNode` extend it.
- `record` — `RecordId`, a validated `record` type used as the leaf value.

Tests are package-private too and rely on direct field access (e.g. `node.maxKeys`, `node.keys`) rather than exposing getters — keep new node classes consistent with this if you want them testable the same way.

## Build, Test, and Development Commands
There is no Maven/Gradle wrapper in this repo — it's a plain IntelliJ IDEA module (`DatabaseFromScratch.iml`, targets `JavaSE-21`). Build and run tests through the IDE's Run/Debug actions (JUnit 5.14.0 is wired up as a test-scope library resolved from the local Maven repository). The `.classpath`/`.project` Eclipse files are legacy and don't match the current `src/main/java` layout — don't rely on them.

## Coding Style & Naming Conventions
No linter or formatter is configured. Follow existing conventions observed in the code:
- Package-private classes and members by default in `index`; expose behavior via methods, not field getters.
- Guard invariants with exceptions in constructors/setters (see `RecordId`, `BPlusTreeNode`) rather than silently clamping values.

## Testing Guidelines
Tests use JUnit 5 (`org.junit.jupiter`) and live in `src/test/java`, mirroring the package of the class under test. Name test methods as behavior descriptions in camelCase (e.g. `rejectsNonPositiveMaximumKeyCounts`, `identifiesAsAnInternalNode`) rather than `test`-prefixed names.

## Commit & Pull Request Guidelines
Commit history is short and descriptive (e.g. "Creating B tree", "Created the abstract class node for leaf node and trees"); one commit uses a `feat:` prefix but this isn't consistently enforced. No PR template or CI exists yet.
