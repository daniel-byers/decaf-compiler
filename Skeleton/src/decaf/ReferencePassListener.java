/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

import org.antlr.v4.runtime.tree.*;

class ReferencePassListener extends DecafParserBaseListener {
  public ReferencePassListener(GlobalScope globalScope, ParseTreeProperty<Scope> scopes) {
    this.globalScope = globalScope;
    this.scopes = scopes;
  } 

  ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
  GlobalScope globalScope;
  Scope currentScope;


  
}

// need main
// arrays only in global, one dimension, fixed size
// identifiers must be declared before use
// No identifier may be defined more than once in the same scope
// An identifier introduced in a method scope can shadow an identifier from the global scope
// identifiers introduced in local scopes shadow identifiers in less deeply nested scopes (up to global).
// "A method can be called only by code appearing after its header."?
// two vaid scopes, global and method
// the global scope consists of names of fields and methods introduced in the class declaration.
// the method scope consists of names of variables and formal parameters introduced in a method declaration.
// Additional local scopes exist within each block in the code; inserted anywhere a statement is legal.
// Assignment is only permitted for scalar values
// field and method names must all be distinct in the global scope
// local variable names and formal parameters names must be distinct in each local scope
// the assignment `location = expr` copies the value resulting from the evaluation of 'expr' into 'location'
// The `location += expr` assignment increments the value stored in location by expr. INT ONLY!
// The `location -= expr` assignment decrements the value stored in location by expr. INT ONLY!
// The 'location' and the 'expr' in an assignment must have the same type.
// For array types, 'location' and 'expr' must refer to a single array element that is also a scalar value.

// the formal arguments of a method are to be like local variables of the method and are initialized,
// by assignment, to the values resulting from the evaluation of the argument expressions.
// The arguments are evaluated from left to right.
// A method that has no declared result type can only be called as a statement, i.e., it cannot be
// used in an expression. Such a method returns control to the caller when return is called (no
// result expression is allowed) or when the textual end of the callee is reached.
// A method that returns a result may be called as part of an expression, in which case the result of
// the call is the result of evaluating the expression in the return statement when this statement is
// reached. It is illegal for control to reach the textual end of a method that returns a result.
// A method that returns a result may also be called as a statement. In this case, the result is ignored.

// It is legal to assign to a formal parameter variable within a method body. affects only the method scope.
//                      ^ what?

// Variable names defined in the method scope or a local scope may shadow method names in the
// global scope. In this case, the identifier may only be used as a variable until the variable leaves
// scope.

// Each location is initialized to a default value when it is declared. Integers have a default value of
// zero, and booleans have a default value of false. Local variables must be initialized when the
// declaring scope is entered. Array elements are initialized when the program starts. 
