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
    // 4. The int_literal in an array declaration must be greater than 0 (negatives fail Parse).
    System.out.println(currentScope + " " + globalScope);
    if (Integer.parseInt(ctx.INTLITERAL().getText()) == 0)
      System.out.println("Error: array " + ctx.IDENTIFIER() + " cannot have a size of zero");
  }

  public void enterMethodDecl(DecafParser.MethodDeclContext ctx) {
    currentScope = scopes.get(ctx);
  }

  public void exitMethodnDecl(DecafParser.MethodDeclContext ctx) {
    currentScope = currentScope.getEnclosingScope();
  }

  public void enterBlock(DecafParser.BlockContext ctx) {
    currentScope = scopes.get(ctx);
  }

  public void exitBlock(DecafParser.BlockContext ctx) {
    currentScope = currentScope.getEnclosingScope();
  }

  public void enterLocation(DecafParser.LocationContext ctx) {
    // 2. No identifier is used before it is declared.
    String locationName = ctx.IDENTIFIER().getText();
    if (currentScope.resolve(locationName) == null)
      System.out.println("Error: variable " + locationName + " cannot be found.");    
  }

  public void enterMethodCall(DecafParser.MethodCallContext ctx) {
    System.out.println("ref-"+currentScope);
    // 2. No identifier is used before it is declared.
    String methodName = ctx.methodName().IDENTIFIER().getText();
    MethodSymbol method = (MethodSymbol) currentScope.resolve(methodName);
    if (method == null) {
      System.out.println("Error: method " + methodName + " cannot be found.");

    // iterate over method's symbols (params) and compare their types to given types of supplied.
    } else {

      List<DecafParser.ExprContext> suppliedParameters = ctx.expr();
      
      int numOfParamsRequired = method.formalParameters.size();
      int numOfParamsSupplied = suppliedParameters == null ? 0 : suppliedParameters.size();

      if (numOfParamsSupplied == numOfParamsRequired) {
        if (numOfParamsRequired != 0) {
          List<Symbol> paramList = new ArrayList<>(method.formalParameters.values());
          ListIterator formalParametersItr = paramList.listIterator();
          ListIterator suppliedParametersItr = suppliedParameters.listIterator();

          List<Symbol.Type> suppliedParametersTypes = new ArrayList<>();
          while (suppliedParametersItr.hasNext()) {
            DecafParser.ExprContext suppliedParameter =
              (DecafParser.ExprContext) suppliedParametersItr.next();

            // if (exprTypes.get(suppliedParameter.expr(0)) != null)
            //   System.out.println("hurray!");
            
            if (arithmaticBinaryOperation(suppliedParameter)) {
              DecafParser.ExprContext lExp = suppliedParameter.expr(0);
              DecafParser.ExprContext rExp = suppliedParameter.expr(1);
              Symbol.Type lExpType = getBinaryOperatorExprType(lExp);
              Symbol.Type rExpType = getBinaryOperatorExprType(rExp);
              // System.out.println("lExpType: " + lExpType);
              // System.out.println("rExpType: " + rExpType);
              if (lExpType != Symbol.Type.INT || rExpType != Symbol.Type.INT)
                System.out.println("Error: bad operand for +");
              else
                suppliedParametersTypes.add(Symbol.Type.INT);
            } 
            else if (suppliedParameter.INTLITERAL() != null) {
              suppliedParametersTypes.add(Symbol.Type.INT);
            }
            else if (booleanBinaryOperation(suppliedParameter)) {
              DecafParser.ExprContext lExp = suppliedParameter.expr(0);
              DecafParser.ExprContext rExp = suppliedParameter.expr(1);
              Symbol.Type lExpType = getBinaryOperatorExprType(lExp);
              Symbol.Type rExpType = getBinaryOperatorExprType(rExp);
              // System.out.println("lExpType: " + lExpType);
              // System.out.println("rExpType: " + rExpType);
              if (lExpType != Symbol.Type.BOOLEAN || rExpType != Symbol.Type.BOOLEAN)
                System.out.println("Error: bad operand for binary operation");
              else
                suppliedParametersTypes.add(Symbol.Type.BOOLEAN);
            }
          }
          System.out.println(suppliedParametersTypes);

          // Now we have a list of the types in the order they appear in the list.
          ListIterator suppliedParametersTypesItr = suppliedParametersTypes.listIterator();

          while (formalParametersItr.hasNext() && suppliedParametersTypesItr.hasNext()) { 
            VariableSymbol formalParameter = (VariableSymbol) formalParametersItr.next();
            Symbol.Type suppliedParameterType = (Symbol.Type) suppliedParametersTypesItr.next();
            if (suppliedParameterType != formalParameter.type) 
              System.out.println("Error: type mismatch; " + suppliedParameterType +
                " given, " + formalParameter.type + " expected.");
          }
          
        }
      } else {
        System.out.println("Error: method " + methodName +
          " takes " + numOfParamsRequired + " parameters. " + numOfParamsSupplied + " given.");
      }


      // if (method.formalParameters != null) {

      //   if (ctx.expr() != null) {
          
      //     for (DecafParser.ExprContext exp : suppliedParametersItr)
          


      //   } else {
      //   }
      // }
    }
  }

          // System.out.println("ref-|"+currentScope+"|"+globalScope);
      // supplied param will be

  public boolean arithmaticBinaryOperation(DecafParser.ExprContext suppliedParameter) {
    return  suppliedParameter.MULTIPLY()    != null
        ||  suppliedParameter.DIVISION()    != null
        ||  suppliedParameter.MODULO()      != null
        ||  suppliedParameter.ADDITION()    != null
        ||  suppliedParameter.MINUS()       != null;
  }

  public boolean booleanBinaryOperation(DecafParser.ExprContext suppliedParameter) {
    return  suppliedParameter.LESSTHAN()    != null
        ||  suppliedParameter.GREATERTHAN() != null
        ||  suppliedParameter.LSSTHNEQTO()  != null
        ||  suppliedParameter.GRTTHNEQTO()  != null
        ||  suppliedParameter.EQUAL()       != null
        ||  suppliedParameter.NOTEQUAL()    != null
        ||  suppliedParameter.AND()         != null
        ||  suppliedParameter.OR()          != null;
  }
  public Symbol.Type getBinaryOperatorExprType(DecafParser.ExprContext ctx) {
    if (ctx.location() != null) {
      String idName = ctx.location().IDENTIFIER().getText();
      Symbol identifier = currentScope.resolve(idName);
      Symbol.Type type = identifier.type;
      return type;
    }
    else if (ctx.INTLITERAL() != null) {
      Token token = ctx.INTLITERAL().getSymbol();
      Symbol.Type type = Symbol.getType(token.getType());
      return type;
    } 
    else if (ctx.BOOLEANLITERAL() != null) {
      Token token = ctx.BOOLEANLITERAL().getSymbol();
      Symbol.Type type = Symbol.getType(token.getType());
      return type;
    } else {
      return null;
    }
  }
  
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
