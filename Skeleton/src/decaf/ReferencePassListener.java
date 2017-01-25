/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;
import java.util.*;
import java6G6Z1010.tools.CLI.*;

class ReferencePassListener extends DecafParserBaseListener {
  public ReferencePassListener(GlobalScope globalScope, ParseTreeProperty<Scope> scopes,
    ParseTreeProperty<Symbol.Type> exprTypes) {
    this.globalScope = globalScope;
    this.scopes = scopes;
    this.exprTypes = exprTypes;
  } 

  ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
  ParseTreeProperty<Symbol.Type> exprTypes = new ParseTreeProperty<>();
  GlobalScope globalScope;
  Scope currentScope;

  public void enterProgram(DecafParser.ProgramContext ctx) {
    currentScope = globalScope;

    // 1. No identifier is declared twice in the same scope.
    for (String variableName : globalScope.getDuplicates())
      System.out.println("Error: name " + variableName + " cannot be defined more than once.");

    // 3. The program contains a definition for a method called main that takes no parameters.
    MethodSymbol mainMethod = (MethodSymbol) currentScope.resolve("main");
    if (mainMethod == null) {
      System.out.println("Error: cannot find main()");
    } else if (!mainMethod.formalParameters.isEmpty()) {
      System.out.println("Error: main() cannot take parameters");
    }
  }

  public void enterArrayDecl(DecafParser.ArrayDeclContext ctx) {
    // 4. The int_literal in an array declaration must be greater than 0 (negatives fail to parse).
    if (Integer.parseInt(ctx.INTLITERAL().getText()) == 0)
      System.out.println("Error: array " + ctx.IDENTIFIER() + " cannot have a size of zero");
  }

  public void enterMethodDecl(DecafParser.MethodDeclContext ctx) {
    currentScope = scopes.get(ctx);
  }

  public void exitMethodDecl(DecafParser.MethodDeclContext ctx) {
    currentScope = currentScope.getEnclosingScope();
  }

  public void enterBlock(DecafParser.BlockContext ctx) {
    currentScope = scopes.get(ctx);
  }

  public void exitBlock(DecafParser.BlockContext ctx) {
    currentScope = currentScope.getEnclosingScope();
  }

  public void enterLocation(DecafParser.LocationContext ctx) {
    String locationName = ctx.IDENTIFIER().getText();
    Symbol location = currentScope.resolve(locationName);


    // 9. An id used as a location must name a declared local/global variable or formal parameter.
    if (location == null)
      System.out.println("Error: variable " + locationName + " cannot be found.");
    else {
      // 10. For all locations of the form id[expr];
      if (ctx.LBRACE() != null && ctx.RBRACE() != null) {
        // a) id must be an array variable,
        if (!(location instanceof ArrayVariableSymbol))
          System.out.println("Error: " + locationName + " is not an array.");
        else {
          Symbol.Type exprType = exprTypes.get(ctx.expr());
          // b) the type of <expr> must be int.
          if (exprType != Symbol.Type.INT)
            System.out.println("Error: array index must be an integer.");
        }
      }
    }
  }

  public void exitStatement(DecafParser.StatementContext ctx) {
    if (ctx.RETURN() != null) {
      // Recursive ascent of the tree from this node is necessary to locate the method in which this
      // return statement has been defined. However, the stack of Scopes cannot be modified directly
      // for this, so a shadow branch is built from this node up to the root that reflects the state
      // of the stack of scopes.
      Scope shadowScope = currentScope;
      while (shadowScope != null ) {
        // We want to stop in the scope before global, which will be the method scope that contains
        // the RETURN statement.
        String nextScopeName = shadowScope.getEnclosingScope().getScopeName();
        if    (nextScopeName == "globals")  break;
        else                                shadowScope = shadowScope.getEnclosingScope();
      }
      // This scope is a MethodSymbol object so also stores information about the return type of the
      // method.
      MethodSymbol method = (MethodSymbol) shadowScope;

      // Therefore, a checks can be made to see if the method is supposed to return a value or not.

      // 7. A return statement must not have a return value unless it appears in the body of a
      // method that is declared to return a value.
      if (method.type == Symbol.Type.VOID && ctx.expr(0) != null)
        System.out.println("Error: method " + method.name + " cannot return an expression");
      else if (method.type != Symbol.Type.VOID) {
        if (ctx.expr(0) == null)
          System.out.println("Error: method " + method.name + " return missing.");
        // 8. The expression in a return statement must have the same type as the declared result
        // type of the enclosing method definition.
        else {
          Symbol.Type exprType = exprTypes.get(ctx.expr(0));
          if (method.type != exprType)
            System.out.println("Error: " + method.name + " returned value must match return type.");
        }
      } 
    }
    else if (ctx.IF() != null) {
      Symbol.Type exprType = exprTypes.get(ctx.expr(0));
      // 11. The expr in an if statement must have type boolean.
      if (exprType != Symbol.Type.BOOLEAN)
        System.out.println("Error: if expression must be a boolean");
    }
    else if (ctx.assignOp() != null) {
      String locationName = ctx.location().IDENTIFIER().getText();
      Symbol location = (Symbol) currentScope.resolve(locationName);
      Symbol.Type exprType = exprTypes.get(ctx.expr(0));

      if (ctx.assignOp().ASSIGNMENT() != null) {

        //  15. The location and expr in an assignment: `location = expr`, must have the same type.
        if (location.type != exprType)
          System.out.println("Error: incompatible types: " + exprType + " cannot be converted to " +
          location.type);
      }
      else if (ctx.assignOp().ASSIGNMENTP() != null || ctx.assignOp().ASSIGNMENTS() != null) {
        // 16. The location and the expr in an incrementing assignment: `location += expr`
        // and decrementing assignment: `location -= expr`, must be of type int.
        if (location.type != Symbol.Type.INT || exprType != Symbol.Type.INT)
          System.out.println(
            "Error: in an incrementing/decrementing assignment, operands must be of type INT; " +
            location.type + " & " + exprType + " given.");
      }
    }
    else if (ctx.FOR() != null) {
      Symbol.Type expr0Type = exprTypes.get(ctx.expr(0));
      Symbol.Type expr1Type = exprTypes.get(ctx.expr(1));

      // 17. The initial expr and the ending expr of for must have type int.
      if (expr0Type != Symbol.Type.INT || expr1Type != Symbol.Type.INT)
        System.out.println("Error: both expressions in for loop must be of type INT");
    }
    else if (ctx.BREAK() != null || ctx.CONTINUE() != null) {
      // All break or continue statements will be in a block of some form; if they're not, they will
      // fail the parsing.
      DecafParser.BlockContext block = (DecafParser.BlockContext) ctx.parent;

      // 18. All break and continue statements must be contained within the body of a for.

      // A for loop is always inside a statement, if the parent of the block node isn't a statement,
      // then the break/continue is not in the right place.
      if  (!(block.parent instanceof DecafParser.StatementContext))
        System.out.println("Error: break and continue must be inside a for loop");
      else {
        // An empty block is also a statement but not a valid place for a break or continue token.
        // So, if the statement that contains the break/continue doesn't also contain a FOR then it
        // is invalid.
        DecafParser.StatementContext statement = (DecafParser.StatementContext) block.parent;
        if (statement.FOR() == null)
          System.out.println("Error: break and continue must be inside a for loop");
      }
    }
  }

  public void enterMethodCall(DecafParser.MethodCallContext ctx) {

    // 2. No identifier is used before it is declared.
    String methodName = ctx.methodName().IDENTIFIER().getText();
    MethodSymbol method = (MethodSymbol) currentScope.resolve(methodName);
    if (method == null) {
      System.out.println("Error: method " + methodName + " cannot be found.");

    // 5. The number and types of arguments in a method call must be the same as the number
    // and  types of the formals, i.e., the signatures must be identical
    } else {

      List<DecafParser.ExprContext> suppliedParameters = ctx.expr();
      
      int numOfParamsRequired = method.formalParameters.size();
      int numOfParamsSupplied = suppliedParameters == null ? 0 : suppliedParameters.size();

      if (numOfParamsSupplied == numOfParamsRequired) {
        if (numOfParamsRequired != 0) {
          List<Symbol> paramList = new ArrayList<>(method.formalParameters.values());
          ListIterator formalParametersItr = paramList.listIterator();
          ListIterator suppliedParametersItr = suppliedParameters.listIterator();

          // Iterate over Method's Symbol list (formal parameters) and compare their types 
          // to given types of supplied arguments that are stored in the ParseTreeProperty object.
          while (formalParametersItr.hasNext() && suppliedParametersItr.hasNext()) { 
            VariableSymbol formalParameter = (VariableSymbol) formalParametersItr.next();

            DecafParser.ExprContext suppliedParameter =
              (DecafParser.ExprContext) suppliedParametersItr.next();

            Symbol.Type suppliedParameterType = exprTypes.get(suppliedParameter);

            if (CLI.debug)
              System.out.println("[supplied: " + suppliedParameterType + "] " +
                "[formal  : " + formalParameter.type + "]");
          
            if (suppliedParameterType != formalParameter.type) 
              System.out.println("Error: type mismatch; " + suppliedParameterType +
                " given, " + formalParameter.type + " expected.");
          }
        }
      } else  System.out.println("Error: method " + methodName + " expects " + 
                numOfParamsRequired + " parameters. " + numOfParamsSupplied + " given.");
    }
  }

  public void exitExpr(DecafParser.ExprContext ctx) {
    if (ctx.methodCall() != null) {
      String methodName = ctx.methodCall().methodName().IDENTIFIER().getText();
      MethodSymbol method = (MethodSymbol) currentScope.resolve(methodName);
      // 6. If a method call is used as an expression, the method must return a result.
      if (method.type == Symbol.Type.VOID)
        System.out.println("Error: method: " + methodName + " is used as an expression, but has " 
          + method.type + " return type.");
    }
    else if (arithmaticBinaryOperation(ctx)) {
      Symbol.Type lExpType = exprTypes.get(ctx.expr(0));
      Symbol.Type rExpType = exprTypes.get(ctx.expr(1));

      // 12. The operands of arith_ops and rel_ops must have type int.
      if (lExpType != Symbol.Type.INT || rExpType != Symbol.Type.INT)
        System.out.println("Error: arithmatic operands must be of type integer.");
    }
    else if (equalityBinaryOperations(ctx)) {
      Symbol.Type lExpType = exprTypes.get(ctx.expr(0));
      Symbol.Type rExpType = exprTypes.get(ctx.expr(1));

      // 13. The operands of eq_ops must have the same type, either int or boolean.
      if (lExpType != rExpType)
        System.out.println("Error: boolean operands must be the same type.");

      if (lExpType != Symbol.Type.INT && lExpType != Symbol.Type.BOOLEAN 
      ||  rExpType != Symbol.Type.INT && rExpType != Symbol.Type.BOOLEAN)
          System.out.println("Error: equality operands must be of type boolean or integer.");
    }
    else if (conditionalBinaryOperation(ctx)) {
      Symbol.Type lExpType = exprTypes.get(ctx.expr(0));
      Symbol.Type rExpType = exprTypes.get(ctx.expr(1));

      // 14. The operands of <ond_ops and the operand of logical not (!) must have type boolean.
      if (lExpType != Symbol.Type.BOOLEAN || rExpType != Symbol.Type.BOOLEAN)
        System.out.println("Error: arithmatic operands must be of type integer.");
    }
    else if (ctx.NOT() != null) {
      Symbol.Type exprType = exprTypes.get(ctx.expr(0));
      // 14. The operands of <ond_ops and the operand of logical not (!) must have type boolean.
      if (exprType != Symbol.Type.BOOLEAN)
        System.out.println("Error: not ( ! ) operand must be of type boolean.");
    }
  }

  // these are ints
  public boolean arithmaticBinaryOperation(DecafParser.ExprContext ctx) {
    return  ctx.MULTIPLY()    != null
        ||  ctx.DIVISION()    != null
        ||  ctx.MODULO()      != null
        ||  ctx.ADDITION()    != null
        ||  ctx.MINUS()       != null
        ||  ctx.LESSTHAN()    != null
        ||  ctx.GREATERTHAN() != null
        ||  ctx.LSSTHNEQTO()  != null
        ||  ctx.GRTTHNEQTO()  != null;
  }

  // these can be ints or bools
  public boolean equalityBinaryOperations(DecafParser.ExprContext ctx) {
    return  ctx.EQUAL()      != null
        ||  ctx.NOTEQUAL()  != null;
  }

  // these must be booleans
  public boolean conditionalBinaryOperation(DecafParser.ExprContext ctx) {
    return  ctx.AND() != null
        ||  ctx.OR()  != null;
  }
  // public Symbol.Type getBinaryOperatorExprType(DecafParser.ExprContext ctx) {
  //   if (ctx.location() != null) {
  //     String idName = ctx.location().IDENTIFIER().getText();
  //     Symbol identifier = currentScope.resolve(idName);
  //     Symbol.Type type = identifier.type;
  //     return type;
  //   }
  //   else if (ctx.INTLITERAL() != null) {
  //     Token token = ctx.INTLITERAL().getSymbol();
  //     Symbol.Type type = Symbol.getType(token.getType());
  //     return type;
  //   } 
  //   else if (ctx.BOOLEANLITERAL() != null) {
  //     Token token = ctx.BOOLEANLITERAL().getSymbol();
  //     Symbol.Type type = Symbol.getType(token.getType());
  //     return type;
  //   } else {
  //     return null;
  //   }
  // }
  
}

  // identifiers must be declared before use
  // identifiers introduced in local scopes shadow identifiers in less deeply nested scopes (up to global).
  // An identifier introduced in a method scope can shadow an identifier from the global scope
  // two vaid scopes, global and method
  // the global scope consists of names of fields and methods introduced in the class declaration.
  // the method scope consists of names of variables and formal parameters introduced in a method declaration.
  // Additional local scopes exist within each block in the code; inserted anywhere a statement is legal.


  // "A method can be called only by code appearing after its header."?
  // Assignment is only permitted for scalar values
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
