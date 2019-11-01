# Black Rook SQL

Copyright (c) 2019 Black Rook Software. All rights reserved.  
[https://github.com/BlackRookSoftware/SQL](https://github.com/BlackRookSoftware/SQL)

[Latest Release](https://github.com/BlackRookSoftware/SQL/releases/latest)

### Required Libraries

NONE

### Required Java Modules

[java.sql](https://docs.oracle.com/en/java/javase/11/docs/api/java.sql/module-summary.html)  

Which requires:

[java.logging](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/module-summary.html)  
[java.transaction.xa](https://docs.oracle.com/en/java/javase/11/docs/api/java.transaction.xa/module-summary.html)  
[java.xml](https://docs.oracle.com/en/java/javase/11/docs/api/java.xml/module-summary.html)  
[java.base](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/module-summary.html)  

### Introduction

This library contains classes for DB functions and query management/building.

### Why?

Everybody deserves a better way to retrieve SQL data away from the boilerplate, as well as
turning the data into Java-friendly structures quickly.

### Library

Contained in this release is a series of classes that are used for DB server functions, plus
object conversion utilities. Supports transactions and annotation-based fetching.

The javadocs contain basic outlines of each package's contents.

### Compiling with Ant

To compile this library with Apache Ant, type:

	ant compile

To make Maven-compatible JARs of this library (placed in the *build/jar* directory), type:

	ant jar

To make Javadocs (placed in the *build/docs* directory):

	ant javadoc

To compile main and test code and run tests (if any):

	ant test

To make Zip archives of everything (main src/resources, bin, javadocs, placed in the *build/zip* directory):

	ant zip

To compile, JAR, test, and Zip up everything:

	ant release

To clean up everything:

	ant clean
	
### Javadocs

Online Javadocs can be found at: [https://blackrooksoftware.github.io/SQL/javadoc/](https://blackrooksoftware.github.io/SQL/javadoc/)

### Other

This program and the accompanying materials are made available under the 
terms of the LGPL v2.1 License which accompanies this distribution.

A copy of the LGPL v2.1 License should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/LICENSE-BlackRookBase.txt).
