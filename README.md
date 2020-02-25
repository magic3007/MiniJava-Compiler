# MiniJava-Compiler

![CI](https://github.com/magic3007/MiniJava-Compiler/workflows/CI/badge.svg?branch=master)

Implementation for the lesson Compiling Engineering (2020 Spring) in Peking University, adjusted from UCLA CS 132 Project..

## Build and Testing

That's easy! Just type these in your terminal:

```
make && make build
```

Type `make test` to run all tests.

## Overview

The works are divided into five parts, each of which is a homework of the course:

1. Semantics analysis: type check a MiniJava program
2. IR generation (1): compile a MiniJava program to Piglet
3. IR generation (2): compile a Piglet program to Spiglet
4. Register allocation: compile a Spiglet program to Kanga
5. Native code generation: compile a Kanga program to MIPS

Below are the tools used in this project. The binaries are under `/deps` folder.

- [JavaCC](https://javacc.github.io/javacc/): an open-source parser generator and lexical analyzer generator written in the Java
- [JTB](http://compilers.cs.ucla.edu/jtb/): a syntax tree builder to be used with JavaCC

## Resources

- [UCLA CS132](http://web.cs.ucla.edu/~palsberg/course/cs132/project.html)
- [Gettysburg CS 374](http://cs.gettysburg.edu/~tneller/cs374/)
