SQL (C) Black Rook Software 
===========================
by Matt Tropiano et al. (see AUTHORS.txt)


Changed in 1.3.0
----------------

- `Added` SQLAbstractDAO.
- `Changed` Some documentation classes and some code redundancy.


Changed in 1.2.5
----------------

- `Fixed` SQLConnection.Transaction did not finish properly.


Changed in 1.2.4
----------------

- `Fixed` SQLConnection.Transaction.isFinished() reported the opposite.


Changed in 1.2.3
----------------

- `Fixed` Potential NPE in SQLRow.getString()


Changed in 1.2.2
----------------

- `Fixed` Releasing a pooled connection did not work. Whoops!


Changed in 1.2.1
----------------

- `Fixed` Updates did not fetch ids properly with DBs that use non-numeric primary keys.
- `Fixed` Removed redundant NClob checking (in places where Clobs are checked for).


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
