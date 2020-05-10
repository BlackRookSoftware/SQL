SQL (C) Black Rook Software 
===========================
by Matt Tropiano et al. (see AUTHORS.txt)


Changed in 1.2.0
----------------

- `Added` "Large" batch calls to separate from "small" batch calls - some Drivers do not implement "large" batches.


Changed in 1.1.1
----------------

- `Fixed` Sometimes a generated id comes back as *any* type in a row without auto-generation.


Changed in 1.1.0
----------------

- `Added` Batching query methods to SQL and SQLConnection.
- `Added` Transaction methods to SQL that do not require SQLConnection from a pool (just a regular Connection). 
- `Added` Exposed some conversion methods in SQL. 
- `Fixed` SQLRow needs to convert Blob/Clob/NClobs while the connection is open. 
- `Changed` Made a few buffer reads more memory-reusing and efficient. 
- `Changed` Stricter resolution of Blob/Clob/NClob conversion in SQLRow (and exception throwing). 


Changed in 1.0.0
----------------

- Base release.
