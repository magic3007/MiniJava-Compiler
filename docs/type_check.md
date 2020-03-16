# MiniJava Compiler: Homework #2 TypeCheck

Because Java is a typed language, we should implement type checking for its subset (i.e., MiniJava).

For simplicity, we say that "type A matches with type B" if any one of the following conditions holds:

- A and B are the same type
- B is the superclass of A

Note that "A matches with B" does not imply "B matches with A".

The type checker should examinate each of the following type constrains:

- The type of a returned expression should match with the type of the method whose body contains the expression.
- The type of the right side of an assignment should match with the type of the left side.
- The type of the condition expression of a "if" statement or a "while" statement should be boolean.
- The types of comparison operants should be integer.
- A method should be invoked with a class type to which the method belongs.
- The type of method operants should match with the signature of that method.

The checker does the type checking in four passes:

1. Collect all the class names.
2. Infer the inheritance relations.
3. Collect all the methods and their signatures.
4. Traverse the AST to compute the type of each expression. In the same pass, the checker also check every type constrain.

