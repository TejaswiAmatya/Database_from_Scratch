1. Why is just nodes not enough?

When we create a B+ tree using Java node objects, the nodes and their data are stored in RAM (memory) while the program is running.
However, when we close the Java application, the memory allocated to those objects is released, meaning all the data stored only in RAM is lost. 
If we restart the application, we would have to create the B+ tree again from scratch. 
This is why binary representation and persistent storage are important when building a database. 
By converting our Java node objects into bytes (serialization) and writing those bytes to a file on disk, 
we can preserve the B+ tree even after the application closes. When we restart the application, 
we can read those bytes from the file and reconstruct the nodes (deserialization). 
Binary representation also gives us more control over storage size, file layout, and disk access. 
We can use both Java objects and binary representation:** Java objects make it easier to perform B+ tree operations in memory, 
while binary representation allows us to efficiently save and retrieve the tree from disk. 
The key point is that binary representation alone does not provide persistence; the bytes must actually be written to disk.