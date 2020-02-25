# MiniJava-Compiler

![CI](https://github.com/magic3007/MiniJava-Compiler/workflows/CI/badge.svg?branch=master)

Implementation for the lesson Compiling Engineering (2020 Spring) in Peking University, adjusted from UCLA CS 132 Project..

## Overview

MiniJava is a subset of Java. In this project, we are going to implement the MiniJava+ compiler. MiniJava+ is a superset of MiniJava but still a subset of Java. The key feature of MiniJava+ compiler is **bootstrapping**. That is, it is written in the source programming language that it intends to compile (i.e. MiniJava+).

The works are divided into five parts, each of which is a homework of the course:

1. Semantics analysis
2. IR generation (1)
3. IR generation (2)
4. Register allocation
5. Native code generation

We manage to achieve a minimal dependence. The tools used in this project are:

- Gradle: a build-automation system
- [JavaCC](https://javacc.github.io/javacc/): an open-source parser generator and lexical analyzer generator written in the Java
- [JTB](http://compilers.cs.ucla.edu/jtb/): a syntax tree builder to be used with JavaCC

## Resources

- [UCLA CS132](http://web.cs.ucla.edu/~palsberg/course/cs132/project.html)
- [Gettysburg CS 374](http://cs.gettysburg.edu/~tneller/cs374/)
