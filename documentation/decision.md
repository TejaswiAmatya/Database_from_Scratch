# 1. Why did we serialize it?
Because if we hadn't serialized it, it would only stay in main memory so serializing would save in files

# 2. Why didn't we simply use the serialization interface?

Because, it doesn't let you control on how you want to serialize a data. For B+ tree you need to have a certain file size
and such.