# B+ tree node serialization: a learning and implementation plan

## What this plan covers

This is the next **binary node format** milestone. The goal is to turn one B+ tree node into exactly 4096 bytes and turn those bytes back into equivalent node data. Do this entirely in memory first. Writing pages to a file, allocating offsets, storing the root location, and reopening a database belong to the later persistent-index milestone.

The playlist description supplied with this project identifies Episode 4 as **“B+Tree node binary representation.”** That is the conceptual topic for this milestone. The byte layout and Java class design below are **proposed Java choices**, not claims about the instructor's exact implementation. If you use the video, check its title/content yourself before relying on a specific detail.

The repository already has `BPlusTreeNode`, `LeafNode`, `InternalNode`, `InMemoryBPlusTree`, and `RecordId`. The existing tree stores child and sibling **Java object references**. Object references cannot be written to disk and later used in a new process. The proposed page format stores **long offsets** for child and sibling links. For now, tests can use sample offsets such as 4096, 8192, and 12288; the future index file will assign real offsets.

> Roadmap note: the repository does not currently show the earlier full-scan-versus-`HashMap` indexing exercise. Complete that exercise if you are following the project's phase order strictly. This plan does not change the in-memory tree or introduce file persistence.

## First, understand the vocabulary

- **Serialization**: converting structured Java data into a defined sequence of bytes.
- **Deserialization**: reading those bytes and reconstructing the structured data.
- **Binary format**: the exact meaning of each byte position. Once files are written, changing this format requires a version or migration plan.
- **Page/block**: a fixed-size chunk of bytes. Here, one node occupies one 4096-byte page, even when it has few entries.
- **Offset**: a byte position in a future file. Offset 4096 means the page starts at byte 4096. It is not a Java array index or a `RecordId`.
- **Header**: the first bytes of a page, carrying information needed to interpret the rest.
- **Payload**: the key/value or key/child entries after the header.
- **Byte order**: the order in which a multi-byte number is stored. Choose `ByteOrder.BIG_ENDIAN` for every page read and write. `ByteBuffer.putInt`, `putLong`, `getInt`, and `getLong` then agree.
- **Sentinel**: a reserved value meaning “no link.” Use `-1L` for an absent *leaf sibling*. Do not use `0`: key `0` and `RecordId(0)` are valid, and offset 0 will be reserved for the future file header.
- **Round trip**: `decode(encode(data))` produces the same logical data. This is the central test for the codec.

For example, Java integer `0x01020304` is four bytes in big-endian order: `01 02 03 04`. A `long` takes eight bytes. A leaf entry with an `int` key and a `long` record ID therefore takes **12 bytes**.

## Why the existing node objects need an adapter

`LeafNode` has private `values` and a `nextLeaf` object reference. `InternalNode` has a list of child object references. A codec should not guess their future file offsets or use `System.identityHashCode`, object serialization, or `toString()`. Those would not give stable, portable page data.

For this milestone, represent the contents of one page with small immutable data holders in `com.tejaswi.database.index`:

```text
LeafPageData:
  keys: List<Integer>
  recordIds: List<RecordId>
  nextLeafOffset: long                 (-1 means no next leaf)

InternalPageData:
  keys: List<Integer>
  childOffsets: List<Long>             (exactly keys.size() + 1)
```

These names are suggestions. Use package-private records or final classes, make defensive `List.copyOf(...)` copies, and validate their invariants in their constructors. The codec works on these page data objects. Later, the storage manager will translate between object links and offsets when it loads or saves tree nodes. Avoid changing `InMemoryBPlusTree` just to make this first codec work.

The boundary is intentional:

```text
Current in-memory node objects
          |
          | future mapping: assign/resolve offsets
          v
LeafPageData / InternalPageData
          |
          | NodeCodec.encode / NodeCodec.decode
          v
Exactly 4096 bytes
```

## Proposed version 1 page layout

All offsets below are **within one 4096-byte page**, beginning at zero. Numbers are big-endian. Bytes not used by the header or payload are zero-filled. The format must be documented before writing codec code; `docs/node-format.md` can later hold the stable format specification while this file remains the learning plan.

```text
Bytes  Size  Field                 Meaning
0..3      4  magic                 0x42504E44 (ASCII "BPND")
4..5      2  version               1
6         1  node type             1 = leaf, 2 = internal
7         1  flags                 0 in version 1
8..11     4  key count             number of stored keys
12..19    8  next leaf offset      leaf: offset or -1; internal: -1
20..23    4  reserved              0 in version 1
24..      -  payload               depends on node type
...    rest  unused bytes          all zero
```

The fixed header is **24 bytes**. Use named constants for every size, field value, and sentinel. Do not scatter unexplained numbers such as `24`, `12`, or `-1` through the codec.

### Leaf payload

After the header, repeat this 12-byte entry `keyCount` times, in strictly ascending key order:

```text
4 bytes: signed int key
8 bytes: non-negative long RecordId.value()
```

The leaf's next-sibling offset is in the header. An empty leaf has `keyCount == 0` and `nextLeafOffset == -1`. Key `0`, negative keys, and record ID `0` remain ordinary data, never sentinels.

Capacity: `(4096 - 24) / 12 = 339` leaf entries. The unused four bytes at the end of a full leaf stay zero.

### Internal payload

An internal node has `keyCount + 1` children. Store the first child offset, then one separator/child pair for each key:

```text
8 bytes: childOffsets[0]
repeat keyCount times:
    4 bytes: signed int separator key
    8 bytes: childOffsets[i + 1]
```

Thus an internal payload takes `8 + 12 * keyCount` bytes. Capacity: `(4096 - 24 - 8) / 12 = 338` separator keys. If one `maxKeys` setting is shared by both node types in a disk-backed tree, cap it at **338**. The current in-memory tree permits other capacities for tests; the codec should reject a page that exceeds this format rather than silently truncate it.

An internal page should have at least one separator and two valid child offsets. Its `nextLeafOffset` header field is always `-1`. The internal separators must be strictly increasing. Each child offset must point to a possible node page: at least 4096 and divisible by 4096. During this phase the codec can validate *shape* and alignment, but it cannot prove that the target page exists; the future storage manager will do that.

### Worked example

Suppose a leaf contains keys `0` and `10`, with record IDs `0` and `25`, and its next leaf will be at offset `8192`:

```text
Header: magic=BPND, version=1, type=leaf, flags=0,
        keyCount=2, nextLeafOffset=8192, reserved=0
Entry 0: key=0,  RecordId=0
Entry 1: key=10, RecordId=25
Remaining 4048 bytes: zero
```

The used length is `24 + 2 * 12 = 48` bytes; `4096 - 48 = 4048` padding bytes remain. This example demonstrates why zero cannot mean “missing entry”: both zero values are real values, and `keyCount` tells the reader precisely how many entries to read.

## File-by-file specification

Use `com.tejaswi.database.index` for every Java file below. Keep the page-data types and codec in this package for now; Java subpackages do not share package-private access. These are specifications to implement one file and its tests at a time, not a request to change the existing in-memory tree. `RecordId` already exists in `com.tejaswi.database.record` and is reused without modification.

### `src/main/java/com/tejaswi/database/index/NodeFormat.java`

**Responsibility:** name the version 1 binary-format constants and calculate page capacities. It has no node data, mutable variables, or I/O. Make it a package-private `final class` with a private constructor. This file is already started; keep its existing constant names.

| Constant | Type | Value or calculation | Purpose |
| --- | --- | --- | --- |
| `PAGE_SIZE` | `int` | `4096` | Bytes in one node page. |
| `HEADER_SIZE` | `int` | `24` | Bytes before the payload. |
| `MAGIC` | `int` | `0x42504E44` | Identifies a B+ tree node (`BPND`). |
| `VERSION` | `short` | `1` | Binary-format version. |
| `LEAF_TYPE`, `INTERNAL_TYPE` | `byte` | `1`, `2` | Values written in header byte 6. |
| `NO_OFFSET` | `long` | `-1L` | No next leaf; never a child offset. |
| `KEY_SIZE` | `int` | `Integer.BYTES` | Size of a key. |
| `RECORD_ID_SIZE`, `CHILD_OFFSET_SIZE` | `int` | `Long.BYTES` | Sizes of a record ID and child offset. |
| `LEAF_ENTRY_SIZE` | `int` | `KEY_SIZE + RECORD_ID_SIZE` | Twelve bytes per leaf entry. |
| `INTERNAL_ENTRY_SIZE` | `int` | `KEY_SIZE + CHILD_OFFSET_SIZE` | Twelve bytes per separator/right-child pair. |
| `MAX_LEAF_ENTRIES` | `int` | `(PAGE_SIZE - HEADER_SIZE) / LEAF_ENTRY_SIZE` | Must equal `339`. |
| `MAX_INTERNAL_KEYS` | `int` | `(PAGE_SIZE - HEADER_SIZE - CHILD_OFFSET_SIZE) / INTERNAL_ENTRY_SIZE` | Must equal `338`. |

When writing the codec, add named zero values for the version 1 flags byte and reserved integer if that makes the header code clearer. Do not store a `maxKeys` instance field in this constants class.

### `src/test/java/com/tejaswi/database/index/NodeFormatTest.java`

**Responsibility:** check the format's intended capacities, `339` and `338`. This file is already started. The expected numbers should be literal values from the documented layout; deriving the expected values with the same formula as `NodeFormat` would make the test circular. There are no instance variables or setup methods needed.

### `src/main/java/com/tejaswi/database/index/LeafPageData.java`

**Responsibility:** hold the logical contents of one leaf page before encoding or after decoding. Use a package-private `record` if you like its generated accessors and equality; a package-private immutable final class is also fine. Do not extend `LeafNode` or hold a `LeafNode` reference.

| Component/field | Type | Meaning |
| --- | --- | --- |
| `keys` | `List<Integer>` | Signed integer keys in strictly increasing order. |
| `recordIds` | `List<RecordId>` | One record ID for each key at the same index. |
| `nextLeafOffset` | `long` | `NodeFormat.NO_OFFSET` or the aligned offset of the next leaf page. |

| Constructor/accessor behavior | Specification |
| --- | --- |
| `LeafPageData(keys, recordIds, nextLeafOffset)` | Reject null lists, null elements, unequal list lengths, duplicate or descending keys, and more than `MAX_LEAF_ENTRIES` entries. Reject an invalid sibling offset. Copy both lists with `List.copyOf` so changes to the caller's lists cannot change the page. |
| `keys()`, `recordIds()`, `nextLeafOffset()` | Return the stored immutable data. A record provides these accessors automatically. Do not add mutable setters. |

A valid sibling offset is `NO_OFFSET` or a value **at least `PAGE_SIZE` and divisible by `PAGE_SIZE`**. An empty leaf is valid with empty lists and `NO_OFFSET`. `0` and negative keys are valid; `RecordId(0)` is valid. `RecordId` itself rejects negative IDs. Validate numeric offsets as offsets, not as record IDs or array positions.

### `src/test/java/com/tejaswi/database/index/LeafPageDataTest.java`

**Responsibility:** test the constructor's invariants. There are no instance variables needed. Include tests for an empty leaf, an ordered leaf containing negative and zero keys and `RecordId(0)`, valid `NO_OFFSET` and `8192` sibling values, defensive list copies, null lists/elements, unequal list lengths, duplicates, descending order, 340 entries, and invalid offsets (`0`, negative values other than `-1`, and unaligned values). Assert that invalid input throws a clear exception. Keep this test independent of `NodeCodec`.

### `src/main/java/com/tejaswi/database/index/InternalPageData.java`

**Responsibility:** hold the logical contents of one internal page. Use a package-private immutable record or final class. It stores numeric child offsets, not `InternalNode` or `BPlusTreeNode` references.

| Component/field | Type | Meaning |
| --- | --- | --- |
| `keys` | `List<Integer>` | Separator keys in strictly increasing order. |
| `childOffsets` | `List<Long>` | Child page offsets; exactly one more than the number of keys. |

| Constructor/accessor behavior | Specification |
| --- | --- |
| `InternalPageData(keys, childOffsets)` | Require at least one key and exactly `keys.size() + 1` child offsets. Reject null lists/elements, duplicate or descending keys, more than `MAX_INTERNAL_KEYS` keys, and invalid child offsets. Copy both lists with `List.copyOf`. |
| `keys()`, `childOffsets()` | Return the stored immutable data. A record provides these accessors automatically. Do not add mutable setters. |

Every child offset must be at least `PAGE_SIZE` and divisible by `PAGE_SIZE`; `NO_OFFSET` is invalid for a child. The constructor checks shape and alignment, not whether another file actually contains those pages. That existence check belongs to the later storage manager.

### `src/test/java/com/tejaswi/database/index/InternalPageDataTest.java`

**Responsibility:** test a one-key/two-child page, multiple ordered keys, key `0`, aligned offsets, defensive copies, and rejection of nulls, missing/extra children, duplicate or descending keys, zero keys, 339 keys, `NO_OFFSET`, `0`, and unaligned child offsets. There are no instance variables needed.

### `src/main/java/com/tejaswi/database/index/NodeCodec.java`

**Responsibility:** translate page data to and from exactly 4096 bytes. Use a package-private `final class` with a private constructor and stateless methods; it needs **no instance variables**. Use a fresh `ByteBuffer` with `ByteOrder.BIG_ENDIAN` for each encode/decode call. The following method names give a concrete API for this milestone:

| Return type | Method | Contract |
| --- | --- | --- |
| `byte[]` | `encodeLeaf(LeafPageData leaf)` | Reject null. Write the version 1 header with leaf type, key count, sibling offset, zero flags/reserved bytes; then write each `int` key and `long` record ID. Return a new 4096-byte array with zero padding. |
| `LeafPageData` | `decodeLeaf(byte[] page)` | Require exactly 4096 bytes and a valid leaf header. Read exactly `keyCount` entries and construct validated `LeafPageData`. Reject malformed values or structure with a clear exception. |
| `byte[]` | `encodeInternal(InternalPageData internal)` | Reject null. Write the internal header with `NO_OFFSET` in its sibling field; write the first child offset, then each separator key/right-child offset pair. Return a new 4096-byte array with zero padding. |
| `InternalPageData` | `decodeInternal(byte[] page)` | Require exactly 4096 bytes and a valid internal header. Read the first child, then exactly `keyCount` key/right-child pairs and construct validated `InternalPageData`. |

Both decoders check magic, version, node type, zero flags, zero reserved field, key count, capacity, and type-specific sibling rules **before** trusting payload positions. They must reject truncated or oversized arrays and invalid data rather than silently returning a partial page. Decide whether to reject nonzero unused padding and use the same rule in both decoders and tests. The codec does not assign offsets, open files, or reconstruct a live tree.

### `src/test/java/com/tejaswi/database/index/NodeCodecTest.java`

**Responsibility:** test bytes as well as round trips. Start with exact leaf header and payload positions, including the worked example above. Then test empty, partial, and full leaves; one-key and full internal pages; zero/negative keys and `RecordId(0)`; and preserved sibling/child offsets. Include at least one **known-byte assertion independent of decoding**, since a matching encoder and decoder could share the same bug. Corrupt copies of valid pages to test wrong magic/version/type, bad counts, nonzero flags/reserved bytes, invalid offsets, unordered keys, negative record IDs, and wrong array lengths. Use JUnit 5; no persistent test fixture or file I/O is needed.

### `docs/node-format.md` (after the codec is complete)

**Responsibility:** become the stable version 1 format reference. Copy the final byte-offset table, payload order, byte order, capacity calculations, sentinel rules, and malformed-page policy from this plan. It has no Java fields or methods. Check that it matches the implemented constants and known-byte tests before calling the binary-format milestone complete.

**Implementation order:** `NodeFormat` + test (started), `LeafPageData` + test, `InternalPageData` + test, leaf encoding/decoding + codec tests, internal encoding/decoding + codec tests, then `docs/node-format.md`. Complete and run each test group before moving to the next file.

## Implementation steps

Complete each step and its tests before starting the next. Paths are relative to the repository root.

### 1. Write down and centralize the format

Create `src/main/java/com/tejaswi/database/index/NodeFormat.java` as a package-private constants holder. Define `PAGE_SIZE`, `HEADER_SIZE`, `MAGIC`, `VERSION`, the two node-type values, `NO_OFFSET`, entry sizes, and calculated maximum counts. The class has no file I/O and no mutable state.

Add `src/test/java/com/tejaswi/database/index/NodeFormatTest.java`. Check that the calculations give 339 leaf entries and 338 internal keys, and that a full page's used length does not exceed 4096. This catches a layout arithmetic error before it becomes a file-format error.

Protected invariants: a persisted node has a known type; its byte representation has a fixed size.

### 2. Define page data and validation

Create `LeafPageData.java` and `InternalPageData.java` beside the codec in `src/main/java/com/tejaswi/database/index/`. Keep them package-private. Their constructors should reject null lists, null elements, mismatched key/value counts, unsorted or duplicate keys, negative record IDs (already rejected by `RecordId`), over-capacity pages, and invalid offsets. A leaf accepts `NO_OFFSET` for its next sibling; an internal node does not accept missing child offsets. Copy incoming lists so later edits cannot change a page being encoded.

Add `LeafPageDataTest.java` and `InternalPageDataTest.java` in the matching test package. Test both valid and invalid examples. A valid empty **leaf** is useful; a zero-key internal node is not needed for this format.

Protected invariants: keys are sorted; leaves have one record ID per key; internals have exactly one more child than keys; links are unambiguous.

### 3. Encode a leaf page

Create `src/main/java/com/tejaswi/database/index/NodeCodec.java`. Start with a method conceptually like `byte[] encodeLeaf(LeafPageData leaf)`. Allocate a fresh 4096-byte `ByteBuffer`, set `BIG_ENDIAN`, write the header in the exact field order above, then write the leaf entries. Java initializes a newly allocated byte buffer to zero, so untouched padding stays zero. Return a 4096-byte array. Never reuse a buffer without resetting it; never write past `PAGE_SIZE`.

Add `src/test/java/com/tejaswi/database/index/NodeCodecTest.java`. Check the output length and inspect a few exact header/payload positions with a big-endian `ByteBuffer`. Include the worked example. Do **not** claim round-trip success yet: that needs the decoder in step 4.

Protected invariants: each node is self-identifying and its actual entries are distinguishable from zero padding.

### 4. Decode a leaf page

Add a decode method that accepts exactly 4096 bytes and returns `LeafPageData` for a leaf. Before reading entries, validate magic, version, type, flags, reserved field, non-negative key count, capacity, and that the stated count fits the page. Read exactly `keyCount` entries. Construct `RecordId` from each stored long. Check ordering and offset validity. Reject nonzero padding if you choose to enforce zero padding, and test that choice consistently. Use a clear exception such as `IllegalArgumentException` for malformed input; later file I/O can wrap it with the file offset for context.

Test empty, one-entry, partly full, and 339-entry leaves. Include negative, zero, `Integer.MIN_VALUE`, and `Integer.MAX_VALUE` keys; `RecordId(0)`; and links `-1` and `8192`. Compare decoded **fields**, not object identity. Mutate encoded bytes to test bad magic, unsupported version, unknown type, impossible count, truncated input, negative record ID, unordered keys, invalid sibling offset, and nonzero reserved bytes.

Protected invariants: corrupt bytes fail clearly instead of producing a plausible but incorrect node.

### 5. Encode and decode internal pages

Add internal-page support to `NodeCodec`. Write the same header with type `2` and `nextLeafOffset == -1`, then the first child offset and each key/right-child pair. Decode in that same order. Return an `InternalPageData`, not a graph of live `InternalNode` objects. Test a one-key page, a multi-key page, and a full 338-key page. Test key `0` as a separator, aligned child offsets, one missing child, duplicate or unsorted separators, invalid type-specific header fields, and an over-capacity count.

The exact return API can be two methods (`decodeLeaf`/`decodeInternal`) or a small sealed page-data interface returned by `decode`. Choose whichever makes callers and tests clear; keep the on-disk bytes identical.

Protected invariants: internal nodes have `keyCount + 1` child references and retain routing order.

### 6. Review the byte format independently

Add at least one test for a **known byte sequence**, rather than only testing `decode(encode(x))`. An encoder and decoder with the same mistake can pass a round-trip test. For example, assert the first four bytes are `42 50 4E 44`, the version is at bytes 4–5, and the worked leaf's first key starts at byte 24. A `ByteBuffer` can read specific offsets with `getInt(24)` and `getLong(28)`.

Review `documentation/serialization.md` against the constants and tests. Once the layout is implemented, copy its stable format details into `docs/node-format.md` as required by the project plan. If code and documentation disagree, correct one before moving on.

## Useful `ByteBuffer` mechanics

```java
ByteBuffer buffer = ByteBuffer.allocate(NodeFormat.PAGE_SIZE);
buffer.order(ByteOrder.BIG_ENDIAN);
buffer.putInt(NodeFormat.MAGIC); // writes four bytes at the current position
buffer.putShort(NodeFormat.VERSION); // writes two bytes and advances position
```

`putInt`/`putLong` advance the buffer's position. `getInt`/`getLong` do the same while reading. `getInt(24)` reads at byte 24 **without** moving the current position. If encoding to `byte[]` using `buffer.array()`, the array is already 4096 bytes; calling `flip()` first is unnecessary for that extraction. If you later pass the `ByteBuffer` itself to a `FileChannel`, call `flip()` after writing so the channel reads from position 0 through the written data, and deliberately decide whether to write the zero padding as well. This milestone does not use `FileChannel`.

Allocate a new buffer per call for now. Avoid Java's built-in object serialization (`ObjectOutputStream`): it does not define the small, stable node format you are trying to learn.

## How to run and judge the tests

In Eclipse, make sure the project uses **JavaSE-21** and JUnit 5. Right-click `NodeCodecTest.java` → **Run As** → **JUnit Test**. Then right-click `src/test/java` or the project → **Run As** → **JUnit Test** to run the existing B+ tree tests too. This repository's IDE module currently names a Java 21 SDK, but its `.classpath`/`.project` files are legacy; if Eclipse does not recognize `src/main/java` and `src/test/java`, configure those as source and test source folders rather than relying on the old entries.

Expected result: every new format and corruption test passes, all existing tree tests still pass, and a decoded page contains the same keys, record IDs, and numeric links as the input page data. The currently existing suite had **56 passing tests** when this plan was written; the count should increase as you add these tests.

If a test fails, first inspect the documented byte offsets and the buffer position after each write. Then check that both paths set the same byte order, that counts mean *keys* rather than bytes, that `childOffsets.size() == keys.size() + 1`, and that `-1` is used only for the absent leaf sibling. A `BufferUnderflowException` or `BufferOverflowException` usually means the count or capacity was not validated before reading or writing; convert malformed input into an explanatory exception.

## Definition of done for this milestone

- [ ] The format is written down with byte offsets, byte order, type values, capacities, and sentinel rules.
- [ ] A leaf page and an internal page each encode to exactly 4096 bytes.
- [ ] Decoding preserves all keys, record IDs, child offsets, and the leaf sibling offset.
- [ ] Empty, partial, and full supported pages have tests.
- [ ] Zero and negative keys are handled as data; `RecordId(0)` works.
- [ ] Malformed pages produce clear exceptions, including wrong magic/version/count and broken ordering.
- [ ] At least one test checks known byte positions independently of a round trip.
- [ ] All existing B+ tree and new JUnit tests pass in Eclipse.
- [ ] `docs/node-format.md` matches the implemented layout.

Suggested commit message after the checklist passes: `feat(index): define and test B+ tree node binary format`.

After this milestone, the next separate task is an index file with a durable file header, aligned node allocation, root-offset persistence, and reopen tests. None of those behaviors is implied by a successful in-memory codec round trip.
