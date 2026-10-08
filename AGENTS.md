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


# Project: Database From Scratch in Java

I am building my own educational, embedded database from scratch in Java.

My goal is not to build a production replacement for PostgreSQL, MySQL, SQLite, or MongoDB. My goal is to deeply understand how database internals work by implementing the core parts myself, incrementally, with tests and documentation.

I am a Computer Science / Software Engineering student with intermediate-to-advanced experience in JavaScript, TypeScript, React, Node.js, PostgreSQL, Prisma, Docker, Python, and backend development. I am newer to Java database internals, binary file storage, B+ trees, index management, bitmaps, caching, locking, and query engines.

---

## Primary learning resource

I am following this YouTube playlist as the primary conceptual guide:

https://www.youtube.com/playlist?list=PLWRwj01AnyEtjaw-ZnnAQWnVYPZF5WayV

Playlist title: **Write a database from scratch** by Code With Sep.

The playlist’s verified topic progression includes:

1. Episode 1 — Introduction to the series
2. Episode 2 — Indexing
3. Episode 3 — B+ Tree
4. Episode 4 — B+Tree node binary representation
5. Episode 5 — Index file, data alignment, and problem with zeros
6. Episode 6 — Index Storage Manager
7. Episode 7 — Deep dive to Index Management, Cache and Locks
8. Episode 9 — Project Updates
9. Episode 10 — Bitmaps and B+Tree List Extension for Non-Unique Indexes
10. Episode 11 — B+Tree Deletion Algorithm
11. Episode 12 — Bitmap in more details
12. Episode 14 — Glue All The Pieces, Database Operations and Review
13. Episode 15 — Showcase
14. Episode 16 — Query, Logical Operations, Offset, Limit, Sort

Do not assume or invent the topics of episodes whose exact title/content has not been verified. If I ask for a plan involving a specific video, inspect/search the playlist or video information first.

Treat the playlist as a conceptual guide. I will implement the concepts in Java, not copy the instructor’s code line by line.

---

## Project target

Build a persistent, embedded Java database with an index-centric architecture.

The finished database should support:

- Persistent records stored on disk
- Persistent collection metadata
- Unique indexes
- Non-unique indexes
- B+ tree indexes
- Binary B+ tree node serialization
- Index files with aligned fixed-size blocks
- An index storage manager
- Index/node cache
- Basic locking for concurrent reads and exclusive writes
- Bitmap-based filtering
- Logical query operations: `AND` and `OR`
- Query sorting
- Offset
- Limit
- Insert
- Find/query
- Update if practical
- Delete
- Restart persistence: close Java process, reopen database, preserve records and indexes
- Database/file integrity verification tools

The first version should be an embedded document/record database—not a complete relational SQL database.

Example target API:

```java
Database db = Database.open(Path.of("./data"));

db.createCollection("students");

db.createIndex("students", "id", true);
db.createIndex("students", "department", false);
db.createIndex("students", "active", false);

RecordId id = db.insert(
    "students",
    Document.of(
        "id", 1,
        "name", "Tejaswi",
        "department", "CS",
        "active", true
    )
);

List<Document> results = db.query(
    "students",
    Query.where("department").equalsTo("CS")
         .and("active").equalsTo(true)
         .sortBy("name", SortOrder.ASC)
         .offset(0)
         .limit(20)
);
```

This exact API can evolve. The underlying learning goals are more important than the syntax.

---

## Explicit non-goals for version 1

Do not push me to implement these unless I explicitly ask for an extension after the core project is complete:

- Full SQL parser
- SQL joins
- Query optimizer
- Query planner
- Foreign keys
- Database networking/server mode
- HTTP API
- WebSockets
- Authentication or authorization
- Admin dashboard
- ORM
- Replication
- Sharding
- Distributed databases
- Multi-machine transactions
- MVCC
- Full ACID transaction engine
- Write-ahead logging / crash recovery
- Full buffer-pool implementation beyond the index/node cache
- Compression
- Encryption
- Secondary indexes beyond the indexes needed for the project
- Production-grade concurrency

Keep the scope educational and finishable.

---

## Required development environment

Use:

- Java 21
- Eclipse IDE
- Standard Java libraries first
- `FileChannel`
- `ByteBuffer`
- `java.nio.file.Path`
- `java.nio.file.Files`
- `ReentrantReadWriteLock`
- `BitSet` initially, followed by a manual bitmap implementation for learning
- JUnit 5 for automated tests
- Git and GitHub for source control

Avoid unnecessary frameworks and dependencies.

Do not require Java modules for this project unless I explicitly ask. Assume there is no `module-info.java`.

---

## Project structure

Use and maintain this package structure:

```text
src/
  main/
    java/
      com/
        tejaswi/
          database/
            Main.java
            api/
            model/
            storage/
            index/
            bitmap/
            cache/
            query/
            util/
    test/
      java/
        com/
          tejaswi/
            database/
              ...
data/
docs/
README.md
```

Package responsibilities:

```text
api/
  Public-facing classes such as Database, Collection, and query-facing interfaces.

model/
  Domain models such as Document, RecordId, IndexDefinition, CollectionMetadata,
  query results, and shared value representations.

storage/
  Binary file access, file headers, record storage, node persistence,
  aligned allocation, and IndexStorageManager.

index/
  B+ tree classes, B+ tree nodes, serialization logic, tree verifier,
  unique indexes, and non-unique indexes.

bitmap/
  Bitmap implementation, bitmap serialization, AND/OR operations,
  record-ID iteration, and bitmap index logic.

cache/
  LRU node cache, dirty tracking, cache eviction, and cache statistics.

query/
  Query object, filters, equality filters, AND/OR filters,
  sorting, offset, limit, and query execution.

util/
  Serialization helpers, byte utilities, input validation, constants,
  error types, and logging helpers.
```

---

## Implementation order

Always guide me through the project in this order unless there is a compelling technical reason to change it.

### Phase 0 — Setup and baseline

- Create Java 21 Eclipse project.
- Use lowercase project name: `database-from-scratch`.
- Do not create `module-info.java`.
- Add `Main.java`.
- Add JUnit 5.
- Initialize Git repository.
- Add `.gitignore`.
- Create `README.md`.
- Create `docs/`.
- Make and run a minimal application.
- Make and run a minimal JUnit test.

### Phase 1 — Indexing concepts

After Episode 1 and Episode 2:

- Implement an in-memory collection/record store.
- Insert records into a `Map`.
- Implement full-scan lookup.
- Implement a simple in-memory index using `HashMap`.
- Compare index lookup with full scan.
- Test duplicate and non-unique values.
- Use a dataset with at least 10,000 records.

The goal is to understand why indexes exist before implementing B+ trees.

### Phase 2 — In-memory B+ tree

After Episode 3:

- Implement an in-memory B+ tree.
- Use integer keys first.
- Use `RecordId` or `long` values.
- Implement leaf nodes.
- Implement internal nodes.
- Implement traversal/search.
- Implement insertion.
- Implement leaf split.
- Implement root split.
- Implement internal-node split.
- Implement leaf sibling links.
- Implement range scan.
- Compare randomized behavior against Java `TreeMap`.

Do not start disk storage until the in-memory B+ tree works and has solid tests.

### Phase 3 — Binary node format

After Episode 4:

- Define fixed-size binary representation for B+ tree nodes.
- Use a 4096-byte node size unless there is a strong reason to choose another size.
- Use `ByteBuffer`.
- Define node header fields.
- Define leaf-node entry layout.
- Define internal-node entry layout.
- Add serialize/deserialize code.
- Test empty, partially full, and full nodes.
- Explicitly test zero values and sentinel values.
- Document the binary layout in `docs/node-format.md`.

### Phase 4 — Persistent index file

After Episode 5:

- Create an `IndexFile`.
- Store a durable file header at byte offset 0.
- Persist root node offset.
- Allocate aligned 4096-byte node blocks.
- Read and write nodes by file offset.
- Validate magic bytes, version, node size, and alignment.
- Avoid using ambiguous zero values as null/sentinel pointers.
- Close and reopen the file in tests.
- Confirm nodes and root offset survive restart.

### Phase 5 — Index storage manager

After Episode 6:

- Create an `IndexStorageManager` abstraction.
- Ensure B+ tree logic does not directly manage `FileChannel`.
- Implement loading nodes.
- Implement saving nodes.
- Implement allocation of leaf/internal nodes.
- Persist disk-backed B+ tree insertion and lookup.
- Persist leaf links.
- Persist root changes after splits.
- Test with at least 1,000 inserted keys across restarts.

### Phase 6 — Cache and locks

After Episode 7:

- Implement a bounded LRU node cache.
- Track cache hits, misses, dirty nodes, evictions, and flushes.
- Flush dirty nodes before eviction.
- Use `ReentrantReadWriteLock`.
- Use read locks for lookup/range scan.
- Use write locks for insert/delete/update.
- Start with tree-level locking only.
- Do not implement fine-grained node-level locking unless the core project is done.
- Add concurrency tests and cache-eviction tests.

### Phase 7 — Persistent records

After the storage/index foundation works:

- Implement `RecordId`.
- Implement an append-only record store.
- Store records/documents on disk.
- Support values: `String`, `Integer`, `Long`, and `Boolean`.
- Add a record format with record ID, payload length, payload bytes, and deletion marker/tombstone.
- Implement insert, get, update, and delete.
- Confirm record persistence across restarts.
- Ensure index values point to valid record IDs.

### Phase 8 — Unique and non-unique indexes

After Episode 10:

- Implement index definitions.
- Implement a unique index: `key -> one RecordId`.
- Reject duplicate values in unique indexes.
- Implement a non-unique index: `key -> many RecordId values`.
- Start with a list/set of record IDs if needed.
- Then implement bitmap-backed non-unique indexes.
- Persist the index data.
- Test insertion, lookup, deletion, and restart persistence.

### Phase 9 — B+ tree deletion

After Episode 11:

- Implement deletion carefully.
- Remove a record from a non-unique index value list/bitmap.
- Remove the B+ tree key only when it has no associated records.
- Implement leaf deletion.
- Implement borrow from siblings.
- Implement leaf merge.
- Update parent separator keys.
- Implement internal-node underflow handling.
- Implement root shrinking.
- Add extensive deterministic and randomized deletion tests.
- Run a tree verifier after deletion tests.

### Phase 10 — Bitmap internals

After Episode 12:

- Implement manual bitmap behavior using `byte[]`.
- Understand byte index and bit index calculations.
- Implement set, get, clear, resize, AND, and OR.
- Compare the custom bitmap implementation with `java.util.BitSet`.
- Serialize and deserialize bitmaps.
- Use bitmap intersection for `AND`.
- Use bitmap union for `OR`.
- Confirm bitmap results match a brute-force full scan.

### Phase 11 — Database integration

After Episode 14:

- Integrate records, collections, indexes, B+ trees, bitmaps, cache, and query execution.
- Create a public `Database` class.
- Support collection creation.
- Support index creation.
- Support inserting documents.
- Support finding documents by unique index.
- Support non-unique indexed filtering.
- Support `AND`.
- Support `OR`.
- Support sorting.
- Support offset.
- Support limit.
- Compare query results with a full-scan reference implementation.

### Phase 12 — Query interface and final polish

After Episode 16:

- Build a simple interactive command shell or Java API.
- Support commands/API operations for create collection, create index, insert, find, query, and delete.
- Add query operators for equality, AND, OR, sort, offset, and limit.
- Do not spend excessive time building a full SQL parser.
- Add database inspection commands.
- Add an integrity checker.
- Finish documentation and final demo.

---

## Testing requirements

Testing is mandatory. Whenever helping me implement a feature, include appropriate JUnit 5 tests.

Every implementation step should include:

1. What class or interface to create.
2. Exact package location.
3. Exact code, when I ask for implementation help.
4. What the code is responsible for.
5. Unit tests to add.
6. How to run the tests in Eclipse.
7. Expected output or behavior.
8. Common mistakes and debugging steps.
9. A small Git commit message after the milestone passes.

Required test categories:

```text
Unit tests
  Test one class or method in isolation.

Persistence tests
  Write data, close database/file, reopen, read data back.

Boundary tests
  Empty node, first record, full node, node split, maximum-sized value,
  zero value, negative value, missing key, duplicate key, deleted record.

Randomized differential tests
  Compare B+ tree behavior with TreeMap.
  Compare index/bitmap query results with full scan.

Integrity tests
  Verify B+ tree ordering, leaf links, parent-child relationships,
  index references, and record existence.

Concurrency tests
  Test multiple readers and controlled writer access after locks are added.
```

Before saying a phase is complete, ask/check whether its tests pass.

---

## Documentation requirements

Keep documentation simple, practical, and updated as the project evolves.

Required files:

```text
README.md
docs/architecture.md
docs/node-format.md
docs/index-file-format.md
docs/record-format.md
docs/decisions.md
docs/known-limitations.md
```

Documentation should explain:

- Overall architecture
- Project goals and non-goals
- How to build and run
- How to run tests
- File and directory layout
- Index-file header
- B+ tree node binary format
- Record binary format
- How node offsets work
- How root persistence works
- How cache eviction works
- How locks work
- How bitmap filtering works
- Known limitations
- Future ideas after version 1

Use diagrams when they clarify storage layout. Prefer simple ASCII diagrams.

Example:

```text
index.idx

Byte 0
+------------------------------------------+
| Index file header                        |
| magic | version | root offset | metadata |
+------------------------------------------+

Byte 4096
+------------------------------------------+
| B+ tree node 1                           |
+------------------------------------------+

Byte 8192
+------------------------------------------+
| B+ tree node 2                           |
+------------------------------------------+
```

---

## Coding standards

Follow these rules:

- Use clear names over clever names.
- Keep classes focused on one responsibility.
- Prefer immutable value objects where practical.
- Use Java records for small immutable data holders such as `RecordId`.
- Use interfaces around major subsystem boundaries.
- Do not introduce design patterns unless they simplify a real problem.
- Avoid global mutable state.
- Validate public method arguments.
- Throw meaningful exceptions.
- Never silently corrupt or ignore malformed data.
- Do not use `System.out.println` as the main debugging strategy; use tests and controlled logging.
- Avoid huge classes.
- Avoid methods longer than necessary.
- Keep binary-format constants centralized.
- Keep file-format version constants explicit.
- Choose one byte order and use it consistently.
- Explain non-obvious byte layout code.
- Prefer `long` for file offsets and persistent record IDs.
- Make resource ownership clear and always close `FileChannel` and other resources.
- Use `try-with-resources` where appropriate.
- Keep production code and test code separate.

---

## Important design invariants

Protect these invariants throughout the project:

```text
1. Every B+ tree node knows whether it is a leaf or internal node.

2. Keys inside every B+ tree node are sorted.

3. B+ tree leaf nodes are linked left-to-right.

4. Every B+ tree leaf is at the same depth.

5. Internal nodes route searches to the correct child.

6. Every persisted node begins at an aligned file offset.

7. The root node location is persisted in the index-file header.

8. Every persisted index pointer refers to a valid stored node or uses a clearly defined sentinel value.

9. Every RecordId referenced by an index points to an existing, non-deleted record unless explicitly allowed by a documented deletion process.

10. Dirty cached nodes are written before eviction and before shutdown.

11. Unique indexes never contain duplicate keys.

12. Non-unique indexes return every matching RecordId exactly once.

13. Bitmap query results must match brute-force full-scan results.

14. Closing and reopening the database must preserve valid committed state.

15. Any detected corruption must produce a clear exception rather than silent incorrect behavior.
```

Whenever giving implementation help, identify which invariant the code protects.

---

## How I want answers

When I ask what to do next:

1. Tell me the exact playlist episode(s) to watch.
2. State the verified topic of each episode.
3. Explain why that episode comes before the implementation task.
4. Give one small, concrete milestone only.
5. Tell me exactly which package, class, interface, or test to create.
6. Give a definition-of-done checklist.
7. Give a suggested Git commit message.
8. Do not jump ahead to future phases.

When I ask for code:

1. Give complete, compilable Java code.
2. Include package declarations.
3. Specify the exact file path for each code block.
4. Include JUnit 5 tests unless I explicitly say not to.
5. Explain each class briefly.
6. Explain how the pieces connect.
7. Keep scope limited to the current milestone.
8. Do not quietly add advanced features not requested.

When I show an error:

1. Explain the likely cause in plain language.
2. Ask for the relevant code/error output only if necessary.
3. Give a minimal fix first.
4. Explain how to prevent the issue later.
5. Do not recommend deleting working code or restarting the project unless truly necessary.

When I ask for architecture guidance:

1. Keep the design consistent with the playlist’s index-centric direction.
2. Clearly distinguish facts from the playlist versus suggested Java adaptations.
3. State tradeoffs.
4. Prefer simple, testable designs.
5. Do not claim a video covers a subject unless it has been verified.

---

## Critical instruction: verify video content

Before telling me:

```text
Watch Episode X, then implement Y.
```

You must verify the topic/title/content of Episode X first.

Do not fabricate a video topic from assumptions about typical database tutorials.

If exact content cannot be verified:

- Say that clearly.
- Tell me to inspect the playlist/video title manually.
- Separate the verified facts from your implementation recommendation.
- Do not present assumptions as facts.

---

## Current starting point

I have created or am creating an Eclipse Java 21 project.

The project should use:

```text
Project name: database-from-scratch
Java version: JavaSE-21
Project layout: separate source and class folders
Java modules: disabled; no module-info.java
```

My first implementation milestone is:

1. Watch Episode 1: Introduction to the series.
2. Watch Episode 2: Indexing.
3. Create a minimal Java application.
4. Create an in-memory record collection.
5. Insert test records.
6. Implement a full scan.
7. Implement a simple `HashMap` index.
8. Prove both approaches return identical results.
9. Add JUnit tests.
10. Commit the milestone.

Suggested initial commit message:

```text
feat(index): compare full scan and in-memory index lookup
```

Do not move me to B+ trees until the indexing milestone is implemented, tested, and understood.