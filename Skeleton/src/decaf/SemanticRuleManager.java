/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 * 
 * and information gained from the following resources:
 * Norling, Dr Emma (2017). Decaf Language Reference. Online. p1-8.
 *
 */

package decaf;

import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.RuleContext;
import java6G6Z1010.tools.CLI.*;
import java.util.*;

class SemanticRuleManager extends DecafParserBaseListener {
  public SemanticRuleManager(ErrorHandler errorHandler) { this.errorHandler = errorHandler; }

  ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
  ParseTreeProperty<Symbol.Type> exprTypes = new ParseTreeProperty<>();
  public ParseTreeProperty<String> exprValues = new ParseTreeProperty<>();
  GlobalScope globalScope;
  Scope currentScope;
  ErrorHandler errorHandler;

  /**
   * Creates the global scope and sets it to the current scope.
   * @param ctx The ProgramContext object defined in DecafParser. Generated at compile time.
   */
  public void enterProgram(DecafParser.ProgramContext ctx) {
    globalScope = new GlobalScope(null);
    currentScope = globalScope;
  }

  /**
   * 3. The program contains a definition for a method called main that takes no parameters.
   * @param ctx The ProgramContext object defined in DecafParser. Generated at compile time.
   */
  public void exitProgram(DecafParser.ProgramContext ctx) {
    MethodSymbol mainMethod = (MethodSymbol) currentScope.resolve("main");
    if (mainMethod == null)
      errorHandler.handleError("cannot find main()", ctx.start);
    else if (!mainMethod.formalParameters.isEmpty())
      errorHandler.handleError("main() cannot take parameters", ctx.start);
    if (CLI.debug) System.out.println(currentScope.toString());
  }

  /**
   * Creates global variable defined in a field declaration and adds them to the current scope.
   * Lists are returned from the DecafParser.java that is generated by ANTLR. List types depend
   * on whether the match was a Lexer Token (TerminalNode) or a Parser Rule (xContext). Iterators
   * are build for these lists so we can later build VariableSymbols from them.
   * @param ctx The FieldDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void enterFieldDecl(DecafParser.FieldDeclContext ctx) {
    ListIterator identifierListItr = ctx.IDENTIFIER().listIterator();
    while (identifierListItr.hasNext()) {
      String identifierName = ( (TerminalNode) identifierListItr.next() ).getText();

      if (currentScope.resolve(identifierName) == null) {
        VariableSymbol newVariableSymbol = 
          new VariableSymbol(identifierName, Symbol.getType(ctx.type().start.getType()));

        currentScope.define(newVariableSymbol);
      }
      else
        errorHandler.handleError(
          "variable '" + identifierName + "' has already been defined.", ctx.start);
    }

    ListIterator arrayidentifierListItr = ctx.arrayDecl().listIterator();
    while (arrayidentifierListItr.hasNext()) {
      DecafParser.ArrayDeclContext arrayDecl =
        (DecafParser.ArrayDeclContext) arrayidentifierListItr.next();

      ArrayVariableSymbol newArrayVariableSymbol = new ArrayVariableSymbol(
        arrayDecl.IDENTIFIER().getText(), Symbol.getType(ctx.type().start.getType()), 
        Integer.parseInt(arrayDecl.INTLITERAL().getText()));
      
      currentScope.define(newArrayVariableSymbol);      
    }
  }

  /**
   *  4. The int_literal in an array declaration must be greater than 0 (negatives fail to parse).
   *  @param  ctx The ArrayDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void enterArrayDecl(DecafParser.ArrayDeclContext ctx) {
    if (Integer.parseInt(ctx.INTLITERAL().getText()) == 0)
      errorHandler.handleError(
        "array '" + ctx.IDENTIFIER() + "' cannot have a size of zero", ctx.start);
  }

  // This is a flag to ensure that if a method is declared to return a type then a return statement
  // is present somewhere in the method declaration. Will be set to true if the method return type
  // is void and also if a return is encountered at some point before method exits.
  boolean returnFound;

  /**
   * Creates MethodSymbols that encapsulate a method, it's formal parameters and "pushes" a new
   * scope to the stack, alongside setting the current scope to the new method. Creates a new 
   * VariableSymbol for each of the method's formal parameters.
   * @param ctx The MethodDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void enterMethodDecl(DecafParser.MethodDeclContext ctx) {
    returnFound = false;
    String methodName = ctx.methodName().IDENTIFIER().getText();

    if (currentScope.resolve(methodName) == null) {
      MethodSymbol newMethodScope = new MethodSymbol(ctx.methodName().IDENTIFIER().getText(), 
        Symbol.getType(ctx.type().get(0).start.getType()), currentScope);

      ListIterator identifierListItr = ctx.IDENTIFIER().listIterator();

      // There is a type for all arguments, but also the method return type. Return type is in index
      // 0, so this iterator starts at index 1 until the end; capturing only the argument types.
      ListIterator identifierTypesItr = ctx.type().listIterator(1);
      while(identifierListItr.hasNext() && identifierTypesItr.hasNext()) {
        VariableSymbol newVariableSymbol = 
          new VariableSymbol(((TerminalNode) identifierListItr.next()).getText(),
          Symbol.getType(((DecafParser.TypeContext) identifierTypesItr.next()).start.getType()));

        newMethodScope.define(newVariableSymbol);

      }
      // If the method doesn't need to return a value then we don't need to look for a return
      if (newMethodScope.type == Symbol.Type.VOID)  this.returnFound = true;
       
      currentScope.define(newMethodScope);
      currentScope = newMethodScope;
    } else {
      // FOUND THE BUG! If a method exists then no new scope is pushed but one is still popped..
      // Create a new pseudo-Scope to counter. This will be popped immediately so has no impact.
      errorHandler.handleError(
        "method '" + methodName + "' has already been defined.", ctx.methodName().start);
      currentScope = new LocalScope(currentScope);
    }
  }

  /**
   * "Pops" the current scope from the stack by setting it to it's enclosing scope.
   * @param ctx The MethodDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void exitMethodDecl(DecafParser.MethodDeclContext ctx) {
    String methodName = ctx.methodName().IDENTIFIER().getText();
    if (!this.returnFound)
      errorHandler.handleError(
        "Method '" + methodName + "' declared to return a value but no return found", ctx.start);

    currentScope = currentScope.getEnclosingScope();
  }

  /**
   * "Pushes" a new local scope for the block and maps the current scope to the ParseTree node.
   * @param ctx The BlockContext object defined in DecafParser. Generated at compile time.
   */
  public void enterBlock(DecafParser.BlockContext ctx) {
    currentScope = new LocalScope(currentScope);
  }

  /**
   * "Pops" the current scope from the stack by setting it to it's enclosing scope.
   * @param ctx The BlockContext object defined in DecafParser. Generated at compile time.
   */
  public void exitBlock(DecafParser.BlockContext ctx) {
    if (CLI.debug) System.out.println(currentScope.toString());
    currentScope = currentScope.getEnclosingScope();
  }

  /**
   * Checks to see if variables have already been defined, and if not creates VariableSymbols for
   * each variable declaration and adds them to the current scope. If the parent scope of current
   * scope is a method and a variable exists with the same name, then an error is raised.
   * void foo(int a) {
   *   int a; // invalid as defined in formal parameters.
   *   {
   *     int a; // valid.
   *     {
   *       int a; // valid.
   *       int a; // invalid as already defined in scope.
   *     }
   *   }
   * }
   * @param ctx The VarDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void enterVarDecl(DecafParser.VarDeclContext ctx) {
    ListIterator identifierListItr = ctx.IDENTIFIER().listIterator();

    while (identifierListItr.hasNext()) {
      String identifierName = ( (TerminalNode) identifierListItr.next() ).getText();

      boolean found = false;
      if      (currentScope.resolveLocal(identifierName) != null) found = true;
      else if (currentScope.getEnclosingScope() instanceof MethodSymbol)
        if (currentScope.getEnclosingScope().resolveLocal(identifierName) != null) found = true;

      if (!found) {
        VariableSymbol newVariableSymbol = new VariableSymbol(identifierName,
            Symbol.getType(ctx.type().start.getType()));

        currentScope.define(newVariableSymbol);
      }
      else
        errorHandler.handleError(
          "variable '" + identifierName + "' has already been defined.", ctx.start);
    }
  }

  /**
   * As a FOR is encountered, a check needs to be made to see if the identifier in the expression
   * has been defined as a Symbol, and if not, create it. The type has to be an Integer.
   * @param ctx The StatementContext object defined in DecafParser. Generated at compile time.
   */
  public void enterStatement(DecafParser.StatementContext ctx) {
    if (ctx.FOR() != null) {
      String identifierName = ctx.IDENTIFIER().getText();
      Symbol identifier = currentScope.resolve(identifierName);
      if (identifier == null)  {
        VariableSymbol newVariableSymbol = new VariableSymbol(identifierName, Symbol.Type.INT);
        currentScope.define(newVariableSymbol);  
      }
    }
  }

  /**
   * @param ctx The StatementContext object defined in DecafParser. Generated at compile time.
   */
  public void exitStatement(DecafParser.StatementContext ctx) {
    if (ctx.RETURN() != null) {
      returnFound = true;
      /*
       * Recursive ascent of the tree from this node is necessary to locate the method in which this
       * return statement has been defined. However, the stack of Scopes cannot be modified directly
       * for this so a shadow branch is built from this node up to the root that mirrors the state
       * of the stack of scopes.
       */
      Scope shadowScope = currentScope;
      while (shadowScope != null ) {
        // We want to stop in the scope before global, which will be the method scope that contains
        // the RETURN statement.
        String nextScopeName = shadowScope.getEnclosingScope().getScopeName();
        if    (nextScopeName == "globals")  break;
        else                                shadowScope = shadowScope.getEnclosingScope();
      }
      // This scope is a MethodSymbol object so also stores information about the return type of the
      // method. Therefore, a checks can be made to see if the method is supposed to return a value.
      MethodSymbol method = (MethodSymbol) shadowScope;

      // 7. A return statement must not have a return value unless it appears in the body of a
      // method that is declared to return a value.
      if (method.type == Symbol.Type.VOID && ctx.expr(0) != null)
        errorHandler.handleError("method '" + method.name + "' cannot return a value", ctx.start);

      else if (method.type != Symbol.Type.VOID) {
        if (ctx.expr(0) == null)
          errorHandler.handleError("method '" + method.name + "' missing RETURN.", ctx.start);
        else {
          Symbol.Type exprType = exprTypes.get(ctx.expr(0));

          // 8. The expression in a return statement must have the same type as the declared result
          // type of the enclosing method definition.
          if (method.type != exprType)
            errorHandler.handleError(
              method.name + " returned value must match return type.", ctx.start);
        }
      } 
    }
    else if (ctx.IF() != null) {
      Symbol.Type exprType = exprTypes.get(ctx.expr(0));
      // 11. The expr in an if statement must have type boolean.
      if (exprType != Symbol.Type.BOOLEAN)
        errorHandler.handleError("IF expression must be a boolean", ctx.start);
    }
    else if (ctx.assignOp() != null) {
      String locationName = ctx.location().IDENTIFIER().getText();
      Symbol location = (Symbol) currentScope.resolve(locationName);
      Symbol.Type exprType = exprTypes.get(ctx.expr(0));

      if (location == null)
        errorHandler.handleError("variable '" + locationName + "' cannot be found.", ctx.start);
      else {
        if (ctx.assignOp().ASSIGNMENT() != null) {

          // 15. The location and expr in an assignment: `location = expr`, must have the same type.
          if (location.type != exprType)
            errorHandler.handleError(
              exprType + " cannot be converted to " + location.type, ctx.expr(0).start);

          /*
           * Technically an array can be assigned to an array index; eg `int x[1], y[2]; x[0] = y`.
           * However, since Decaf doesn't allow multi-dimensional arrays, an array index assignment
           * must be validated to ensure another array isn't the expr being assigned.
           */
          if (ctx.expr(0).location() != null) {
            Symbol variable = currentScope.resolve(ctx.expr(0).location().IDENTIFIER().getText());

            if (location instanceof ArrayVariableSymbol && variable instanceof ArrayVariableSymbol){
            /*
             * Both sides of the assignment are arrays, but the right hand side can't be an array if
             * the array indexes aren't present:
             * eg. `a[1] = a;` is invalid, but `a[1] = a[2]` is valid.
             * Also need to ensure that if the braces for one side aren't present, the other side
             * doesn't have them too. eg. `int a[1], b[1]; a = b;` should be a valid expression.
             * So checks need to be done to ensure the sizes of the arrays are equal as well.
             * eg. `int a[5], b[3]; a = b;` should be invalid.
             */ 

              // Check if LHS Array is index or not
              boolean lhsIsIndex =     ctx.location().LBRACE() != null
                                    && ctx.location().RBRACE() != null;
              // Check if RHS Array is index or not
              boolean rhsIsIndex =     ctx.expr(0).location().LBRACE() != null 
                                    && ctx.expr(0).location().RBRACE() != null;

              if      (lhsIsIndex && !rhsIsIndex)
                errorHandler.handleError("multi-dimensional arrays are not permitted.", ctx.start);

              else if (((ArrayVariableSymbol) location).size != ((ArrayVariableSymbol) variable).size)
                errorHandler.handleError(
                  "arrays can only be assigned to similar sized arrays.", ctx.start);

              else if (!lhsIsIndex && rhsIsIndex)
                errorHandler.handleError(
                  exprType + " cannot be converted to " + location.type + "[]", ctx.start);
            }
          }

          /*
           * Need to check to ensure array index assignments aren't explicitly out of bounds.
           * Three possible, explicit, cases;
           * 1) lhs of assignment is array with scalar index and rhs is scalar    `a[1] = 5;`
           * 2) lhs of assignment is array with scalar index and rhs is variable  `a[1] = b;`
           * 3) lhs of assignment is variable and rhs is array with scalar index  `b    = a[1];`
           */
          // Mock object to avoid null pointers in case 2.
          Object variable = new Object();

          if (ctx.expr(0).location() != null)
            variable = (Symbol) currentScope.resolve(ctx.expr(0).location().IDENTIFIER().getText());

          boolean lhsArray = location instanceof ArrayVariableSymbol;
          boolean rhsArray = variable instanceof ArrayVariableSymbol;

          // Check left hand side array index isn't variable or explicitly out of bounds
          if      (lhsArray && ctx.location().expr().location() == null) {
            int arrayIndex = Integer.parseInt(ctx.location().expr().INTLITERAL().getText());
            int arraySize = ( (ArrayVariableSymbol) location ).size;

            if (arrayIndex >= arraySize)
              errorHandler.handleError("Array index out of bounds! Size: " + arraySize, ctx.start);
          }
          // Check right hand side array index isn't explicitly out of bounds
          else if (rhsArray) {
            try {
              int arrayIndex = Integer.parseInt(ctx.expr(0).location().expr().getText());
              int arraySize = ( (ArrayVariableSymbol) variable ).size;
                
              if (arrayIndex >= arraySize)
                errorHandler.handleError("Array index out of bounds! Size: " + arraySize, ctx.start);
            } catch (NumberFormatException nfe) { } // Expr in left hand side is not a number
          }

        }
        else if (ctx.assignOp().ASSIGNMENTP() != null || ctx.assignOp().ASSIGNMENTS() != null) {
          // 16. The location and the expr in an incrementing assignment: `location += expr`
          // and decrementing assignment: `location -= expr`, must be of type int.
          if (location.type != Symbol.Type.INT || exprType != Symbol.Type.INT)
            errorHandler.handleError("operands must be of type INT; " + location.type + " & "
              + exprType + " given.", ctx.start);
        }
      }
    }
    else if (ctx.FOR() != null) {
      Symbol.Type expr0Type = exprTypes.get(ctx.expr(0));
      Symbol.Type expr1Type = exprTypes.get(ctx.expr(1));

      // 17. The initial expr and the ending expr of for must have type int.
      if      (expr0Type != Symbol.Type.INT)
        errorHandler.handleError("operand in FOR must be of type INT", ctx.expr(0).start);
      else if (expr1Type != Symbol.Type.INT)
        errorHandler.handleError("operand in FOR must be of type INT", ctx.expr(1).start);
    }
    else if (ctx.BREAK() != null || ctx.CONTINUE() != null) {
      /*
       * At this point a break/continue statement has been encountered which has to be in a FOR-loop
       * A FOR-loop is always inside a method, if we ascend the tree from this statement 
       * and reach a method declaration without first encountering a for, then we must assume that
       * the for isn't present and the break/continue is not in the right place.
       */
      boolean foundFor = false;
      RuleContext shadowCtx = ctx;
      while (shadowCtx != null) {
        // If RuleContext is a StatementContext then see if the FOR() is null
        if (shadowCtx instanceof DecafParser.StatementContext) 
          if (((DecafParser.StatementContext) shadowCtx).FOR() != null)  foundFor = true;
        
        // Continue until we reach a method declaration.
        if    (shadowCtx instanceof DecafParser.MethodDeclContext)  break;
        else                                                        shadowCtx = shadowCtx.parent;
      }

      // 18. All break and continue statements must be contained within the body of a for.
      if (!foundFor)  errorHandler.handleError("BREAK/CONTINUE must be inside a FOR", ctx.start); 
    }
  }

  /**
   * @param ctx The MethodCallContext object defined in DecafParser. Generated at compile time.
   */
  public void exitMethodCall(DecafParser.MethodCallContext ctx) {
    if (ctx.CALLOUT() != null) {
      if (CLI.debug)
        System.out.println("CALLOUT to library function: " + ctx.STRINGLITERAL().getText());
    } else {
      // 2. No identifier is used before it is declared.
      String methodName = ctx.methodName().IDENTIFIER().getText();
      MethodSymbol method = (MethodSymbol) currentScope.resolve(methodName);
      if (method == null) { 
        errorHandler.handleError("method '" + methodName + "' cannot be found.", ctx.start);
      } else {
        List<DecafParser.ExprContext> suppliedParameters = ctx.expr();
        // 5. The number and types of arguments in a method call must be the same as the number
        // and  types of the formals, i.e., the signatures must be identical
        int numOfParamsRequired = method.formalParameters.size();
        int numOfParamsSupplied = suppliedParameters == null ? 0 : suppliedParameters.size();
        
        if (numOfParamsSupplied == numOfParamsRequired) {
          if (numOfParamsRequired != 0) {
            List<Symbol> paramList = new ArrayList<>(method.formalParameters.values());
            ListIterator formalParametersItr = paramList.listIterator();
            ListIterator suppliedParametersItr = suppliedParameters.listIterator();

            while (formalParametersItr.hasNext() && suppliedParametersItr.hasNext()) { 
              VariableSymbol formalParameter = (VariableSymbol) formalParametersItr.next();

              DecafParser.ExprContext suppliedParameter =
                (DecafParser.ExprContext) suppliedParametersItr.next();

              Symbol.Type suppliedParameterType = exprTypes.get(suppliedParameter);

              if (suppliedParameterType != formalParameter.type) 
                errorHandler.handleError("type mismatch; " + suppliedParameterType +
                  " given, " + formalParameter.type + " expected.", suppliedParameter.start);
            }
          }
        }
        else  errorHandler.handleError("method '" + methodName + "' expects " + 
                  numOfParamsRequired + " parameters. " + numOfParamsSupplied + " given.",
                  ctx.start);
      }
    }
  }

  /**
   * @param ctx The LocationContext object defined in DecafParser. Generated at compile time.
   */
  public void exitLocation(DecafParser.LocationContext ctx) {
    String locationName = ctx.IDENTIFIER().getText();
    Symbol location = currentScope.resolve(locationName);

    // 9. An id used as a location must name a declared local/global variable or formal parameter.
    if (location == null)
      errorHandler.handleError("variable '" + locationName + "' cannot be found.", ctx.start);
    else {
      // 10. For all locations of the form id[expr];
      if (ctx.LBRACE() != null && ctx.RBRACE() != null) {
        // a) id must be an array variable,
        if (!(location instanceof ArrayVariableSymbol))
          errorHandler.handleError("'" + locationName + "' is not an array.", ctx.start);
        else {
          Symbol.Type exprType = exprTypes.get(ctx.expr());
          // b) the type of <expr> must be int.
          if (exprType != Symbol.Type.INT)
            errorHandler.handleError("array index must be an INT.", ctx.start);
        }
      }
    }
  }

  /**
   * Whenever an expr is encountered, it's type is added to the exprTypes HashMap, and it's value
   * is added to the exprValues HashMap located in Main.
   * @param ctx The ExprContext object defined in DecafParser. Generated at compile time.
   */
  public void enterExpr(DecafParser.ExprContext ctx) {
    if      (ctx.location()       != null)  {
      Symbol identifier = currentScope.resolve(ctx.location().IDENTIFIER().getText());
      if    (identifier           != null)  exprTypes.put(ctx, identifier.type);
      else                                  exprTypes.put(ctx, Symbol.Type.INVALID);
    }
    else if (ctx.INTLITERAL()     != null) {
      exprTypes.put(ctx, Symbol.Type.INT);
      Main.exprValues.put(ctx, ctx.INTLITERAL().getText());
    }
    else if (ctx.BOOLEANLITERAL() != null) {
      exprTypes.put(ctx, Symbol.Type.BOOLEAN);
      Main.exprValues.put(ctx, ctx.BOOLEANLITERAL().getText());
    }
    else if (ctx.CHARLITERAL()    != null) { 
      exprTypes.put(ctx, Symbol.Type.INT);
      Main.exprValues.put(ctx, ctx.CHARLITERAL().getText().substring(1,2));
    }
    else if (ctx.methodCall()     != null) {
      // Callouts return type INT. So the type of the expression containing it will be INT too.
      if    (ctx.methodCall().CALLOUT() != null) exprTypes.put(ctx, Symbol.Type.INT);
      else  {
        exprTypes.put(ctx,
          currentScope.resolve(ctx.methodCall().methodName().IDENTIFIER().getText()).type);
      }
    }
  }

  /**
   * @param ctx The ExprContext object defined in DecafParser. Generated at compile time.
   */
  public void exitExpr(DecafParser.ExprContext ctx) {
    if (ctx.methodCall() != null) {
      if (ctx.methodCall().CALLOUT() == null) {
        String methodName = ctx.methodCall().methodName().IDENTIFIER().getText();
        MethodSymbol method = (MethodSymbol) currentScope.resolve(methodName);
        // 6. If a method call is used as an expression, the method must return a result.
        if (method == null)
          errorHandler.handleError("method '" + methodName + "' cannot be found.", ctx.start);
        else
          if (method.type == Symbol.Type.VOID)
            errorHandler.handleError("method '" + methodName + "' is used as an expression, but has" 
              + " " + method.type + " return type.", ctx.start);
      }
    }
    // This is the case when it's '-1' instead of 'x - 1'; there is only one expression
    else if (ctx.MINUS() != null && ctx.expr().size() == 1) {
      Symbol.Type minusExprType = exprTypes.get(ctx.expr(0));
      if    (minusExprType == Symbol.Type.INT)  exprTypes.put(ctx, Symbol.Type.INT);
      else                                      exprTypes.put(ctx, Symbol.Type.INVALID);
    }
    else if (ctx.NOT() != null) {
      Symbol.Type notExprType = exprTypes.get(ctx.expr(0));
      if  (notExprType == Symbol.Type.BOOLEAN)  exprTypes.put(ctx, Symbol.Type.BOOLEAN);
      else { 
        errorHandler.handleError("NOT ( ! ) operand must be of type BOOLEAN.", ctx.expr(0).start);
        exprTypes.put(ctx, Symbol.Type.INVALID);
      }
    }
    else if (ctx.LPAREN() != null && ctx.RPAREN() != null) {
      Symbol.Type exprType = exprTypes.get(ctx.expr(0));
      exprTypes.put(ctx, exprType);
    }
    else if (ExpressionOperationRules.arithmeticBinaryOperation(ctx)) {
      Symbol.Type lExprType = exprTypes.get(ctx.expr(0));
      Symbol.Type rExprType = exprTypes.get(ctx.expr(1));

      // 12. The operands of arith_ops and rel_ops must have type int.
      if (lExprType == Symbol.Type.INT && rExprType == Symbol.Type.INT) {
        if      (ExpressionOperationRules.arithmeticReturnsInteger(ctx))
          exprTypes.put(ctx, Symbol.Type.INT);
        else if (ExpressionOperationRules.arithmeticReturnsBoolean(ctx))
          exprTypes.put(ctx, Symbol.Type.BOOLEAN);
      } else {
        exprTypes.put(ctx, Symbol.Type.INVALID);
        errorHandler.handleError("arithmetic operands must be of type INT.", ctx.start);
      }
    }
    else if (ExpressionOperationRules.equalityBinaryOperations(ctx)) {
      Symbol.Type lExprType = exprTypes.get(ctx.expr(0));
      Symbol.Type rExprType = exprTypes.get(ctx.expr(1));

      // 13. The operands of eq_ops must have the same type, either int or boolean.
      if (lExprType != rExprType)
        errorHandler.handleError("BOOLEAN operands must be the same type.", ctx.start);

      if (lExprType == Symbol.Type.BOOLEAN && rExprType == Symbol.Type.BOOLEAN
      ||  lExprType == Symbol.Type.INT     && rExprType == Symbol.Type.INT)
        exprTypes.put(ctx, Symbol.Type.BOOLEAN);
      else {
        errorHandler.handleError("equality operands must be of type BOOLEAN or INT.", ctx.start);
        exprTypes.put(ctx, Symbol.Type.INVALID);
      }
    }
    else if (ExpressionOperationRules.conditionalBinaryOperation(ctx)) {
      Symbol.Type lExprType = exprTypes.get(ctx.expr(0));
      Symbol.Type rExprType = exprTypes.get(ctx.expr(1));

      // 14. The operands of cond_ops and the operand of logical not (!) must have type boolean.
      if (lExprType == Symbol.Type.BOOLEAN && rExprType == Symbol.Type.BOOLEAN)
        exprTypes.put(ctx, Symbol.Type.BOOLEAN);
      else {
        errorHandler.handleError("logical operands must be of type BOOLEAN.", ctx.start);
        exprTypes.put(ctx, Symbol.Type.INVALID);
      }
    }
  }
}