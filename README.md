# Interpreter Development Project

This repository contains the implementation of a custom programming language and its corresponding interpreter, developed as part of the academic curriculum. The project focuses on the core principles of compiler construction, including lexical, syntax, and semantic analysis, as well as intermediate code generation and interpretation.

## Language Specification

The designed language is a **statically typed**, **functional**, and user-friendly programming language. It is built to be intuitive while supporting fundamental programming constructs.

### Data Types and Typing
* **Static Typing:** All variable types are determined at compile-time.
* **Integers:** Support for whole numbers.
* **Booleans:** Support for logical values (truth values may be represented by numerical types).
* **Arrays:** Support for collections of data elements.

### Operators
| Category | Description |
| :--- | :--- |
| **Assignment** | Operators for assigning values to variables. |
| **Arithmetic** | Support for at least one arithmetic operation. |
| **Relational** | Comparison operators for logical branching. |
| **Logical** | Support for logical operations with **short-circuiting** behavior (e.g., in `A || B`, `B` is not evaluated if `A` is true). |

### Control Flow & Functions
* **Branching:** Implementation of `if` and `else` (alternative) statements.
* **Loops:** Support for at least two different types of iteration structures.
* **Functions:** A core component of the language consisting of:
    * **Input:** Defined parameters.
    * **Output:** A specified return type.
    * **Body:** A structured list of statements to be executed.

## Technical Implementation

The project is implemented with a focus on modularity and clear separation of concerns across the different stages of interpretation.

### Tools and Technologies
* **Implementation Language:** Java
* **Parser Generation:** ANTLR
* **Environment:** Support for command-line arguments and standard output (stdout).

## Team Information

* **Marko Stojicic:**
* **Vidan Stojic**

*Developed as part of the Programming Translators course.*
