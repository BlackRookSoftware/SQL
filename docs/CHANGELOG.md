SQL (C) Black Rook Software 
===========================
by Matt Tropiano et al. (see AUTHORS.txt)


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
