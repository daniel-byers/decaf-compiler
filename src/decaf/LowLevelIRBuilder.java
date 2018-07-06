/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence. (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 *
 * and information gained from the following resources:
 * [1] Aiken, Professor Alex. (2012). Compilers Stanford (Playlist). [Video files]. Retrieved from 
 * https://www.youtube.com/playlist?list=PLFB9EC7B8FE963EB8. Last accessed 22nd Mar 2017.
 *
 * AMD (2013). AMD64 Architecture Programmer’s Manual Volume 1: Application Programming. Rev 3.20. 
 * Online. p23-109.
 *
 * Bacon, Jason W.. (2011). Arrays in Assembly Language: Chapter 12. Memory and Arrays. Available: 
 * http://www.cs.uwm.edu/classes/cs315/Bacon/Lecture/HTML/ch12s04.html. Last accessed 22nd Mar 2017.
 *
 * Intel (2016). Intel 64 and IA-32 Architectures Software Developer’s Manual. Online. p113-118.
 *
 * Thain, Prof. Douglas. (2015). Introduction to X86-64 Assembly for Compiler Writers. Available: 
 * https://www3.nd.edu/~dthain/courses/cse40243/fall2015/intel-intro.html.
 * Last accessed 22nd Mar 2017.
 *
 */

/*
  TODO:  Code generator should emit code to perform these checks:
  --      1. The subscript of an array must be in bounds. (BOUNDS command)
  --      2. Control must not fall off the end of a method that is declared to return a result.
  --     NOT (!) and urany MINUS (-x) need implementing
*/

package decaf;

import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.*;
import java.util.*;
import java.io.*;

class LowLevelIRBuilder extends DecafParserBaseListener {
  public LowLevelIRBuilder() {
    charList = new ArrayList<>();
    for(char alphabet = 'a'; alphabet <= 'z'; alphabet++) charList.add(alphabet);
    charListItr = charList.listIterator();
    variableRegisterMap = new HashMap<>();
    exprResultRegisterMap = new ParseTreeProperty<>();
    blockIndexes = new ParseTreeProperty<>();
    blockIndexes_2 = new ParseTreeProperty<>();
  }

  private int registerCounter; 
  private int ifLabelCounter;
  private int forLabelCounter;
  private List<Character> charList;
  private ListIterator charListItr;
  private Map<String, String> variableRegisterMap;
  private ParseTreeProperty<String> exprResultRegisterMap; 
  private ParseTreeProperty<Integer> blockIndexes, blockIndexes_2;

  // This holds a list of ThreeCodeTuples in order that they are encountered in the tree, therefore,
  // also the order in which they appear in the Decaf source code. Each callback will be responsible
  // for adding however many ThreeCodeTuple objects to the InstructionSet that are required to do 
  // the task it's designed to do. After complete traversal of the ParseTree, a list of instructions
  // will represent the sequence of low level instructions to be generated.
  InstructionSet programInstructionSet = new InstructionSet();

  ArrayList<ThreeCodeTuple> dataSegment = new ArrayList<>();

  /**
   * Adds the label denoting the start of a new method so as to allows jumps later.
   * @param ctx The MethodDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void enterMethodDecl(DecafParser.MethodDeclContext ctx) {
    String methodName = ctx.methodName().IDENTIFIER().getText();
    programInstructionSet.addInstruction(labelTuple(methodName + ":"));
  }

  /**
   * Adds a return statement at the end of every method (except main) so that control returns to the
   * point on which it was called after the method body executes.
   * @param ctx The MethodDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void exitMethodDecl(DecafParser.MethodDeclContext ctx) {
    if (!ctx.methodName().IDENTIFIER().getText().equals("main"))
      programInstructionSet.addInstruction(returnTuple());
  }

  /**
   * Calculates the index of the instruction set of where the block exits so that else block code
   * can be inserted in the correct order.
   * @param ctx The BlockContext object defined in DecafParser. Generated at compile time.
   */
  public void enterBlock(DecafParser.BlockContext ctx) {
    blockIndexes.put(ctx, programInstructionSet.instructions.size());
  }
  
  /**
   * This method is required to calculate where to insert jump to else statements when there is no
   * else block.
   * @param ctx The BlockContext object defined in DecafParser. Generated at compile time.
   */
  public void exitBlock(DecafParser.BlockContext ctx) {
    blockIndexes_2.put(ctx, programInstructionSet.instructions.size());
  }

  /**
   * Adds each location to the register map so the current temporary register holding a variable is
   * mapped to the name of the variable.
   * @param ctx The StatementContext object defined in DecafParser. Generated at compile time.
   */
  public void enterStatement(DecafParser.StatementContext ctx) {
    if (ctx.FOR() != null) {
      String locationName = ctx.IDENTIFIER().getText();
      String r0 = nextRegister();
      variableRegisterMap.put(locationName, r0);
    }
  }

  /**
   * Controls the logic for statements in Decaf.
   * @param ctx The StatementContext object defined in DecafParser. Generated at compile time.
   */
  public void exitStatement(DecafParser.StatementContext ctx) {
    if (ctx.assignOp() != null) {

      String locationName = ctx.location().IDENTIFIER().getText();
      String locationReg = variableRegisterMap.get(locationName);
      String v0 = getExprValue(ctx.expr(0));

      if      (ctx.assignOp().ASSIGNMENTP() != null) 
        programInstructionSet.addInstruction(additionTuple(v0, locationReg));
      else if (ctx.assignOp().ASSIGNMENTS() != null)
        programInstructionSet.addInstruction(subtractionTuple(v0, locationReg));
      else if (ctx.location().LBRACE() != null && ctx.location().RBRACE() != null) { // lhs array
        String arrayIndex = getExprValue(ctx.location().expr());

        addArrayIndexAddressToPointer("array_" + locationName, arrayIndex);

        programInstructionSet.addInstruction(moveTuple(v0, "(%rbp)"));
      }
      else if (ctx.expr(0).location() != null) {
        // rhs array
        if (ctx.expr(0).location().LBRACE() != null && ctx.expr(0).location().RBRACE() != null) {
          String arrayIndex = getExprValue(ctx.expr(0).location().expr());
          locationName = ctx.expr(0).location().IDENTIFIER().getText();

          addArrayIndexAddressToPointer("array_" + locationName, arrayIndex);

          programInstructionSet.addInstruction(moveTuple("(%rbp)", locationReg));
        }
      }
      else
        programInstructionSet.addInstruction(moveTuple(v0, locationReg));

    }
    else if (ctx.RETURN() != null) {
      String v0 = getExprValue(ctx.expr(0));
      programInstructionSet.addInstruction(moveTuple(v0, "%rax"));
    }
    else if (ctx.FOR() != null) {
      String locationName = ctx.IDENTIFIER().getText();
      String r0 = variableRegisterMap.get(locationName);
      String r1 = nextRegister();      
      
      String v0 = getExprValue(ctx.expr(0));
      String v1 = getExprValue(ctx.expr(1));
      
      String currentForNumber = nextForLabelNumber();
      int forBlock = blockIndexes.get(ctx.block(0));
      ArrayList<ThreeCodeTuple> tmp = new ArrayList<>();

      // Creates a temporary sub list of instructions that will be inserted into the list at the
      // index of the FOR-loop block
      tmp.add(moveTuple(v0, r0));
      tmp.add(moveTuple(v1, r1));
      tmp.add(labelTuple("startfor_" + currentForNumber + ":"));
      
      programInstructionSet.addMultipleInstructions(tmp, forBlock);
      programInstructionSet.addInstruction(additionTuple("$1", r0));
      programInstructionSet.addInstruction(cmpTuple(r0, r1));
      programInstructionSet.addInstruction(jumpNotEqualTuple("startfor_" + currentForNumber));
      programInstructionSet.addInstruction(labelTuple("endfor_" + currentForNumber + ":"));
    }
    else if (ctx.BREAK() != null) {
      // The break always hits before the forLabelCounter is incremented so 1 is added to balance.
      programInstructionSet.addInstruction(
        jumpTuple("endfor_" + Integer.toString(forLabelCounter + 1)));
    }
    else if (ctx.CONTINUE() != null) {
      // The continue always hits before the forLabelCounter is incremented so 1 is added to balance
      programInstructionSet.addInstruction(
        jumpTuple("startfor_" + Integer.toString(forLabelCounter + 1)));
    }
    else if (ctx.IF() != null) {
      String currentIfElseNumber = nextIfLabelNumber();
      int elseBlockOffset = 0;
    
      String v0 = getExprValue(ctx.expr(0));
      String r0 = nextRegister();
      ArrayList<ThreeCodeTuple> tmp = new ArrayList<>();

      tmp.add(moveTuple(v0, r0));
      tmp.add(cmpTuple("$0", r0));

      // Always need a jump-to-else jump regardless of whether there is an else block or not. This 
      // stops code being executed under the condition that the 'IF expression' evaluated to false.
      // Technically, this is a jump-to-endif as there is no 'else' block, however, as label names
      // are arbitrary, the point is moot. 
      tmp.add(jumpEqualTuple("else_" + currentIfElseNumber));
      tmp.add(labelTuple("if_" + currentIfElseNumber + ":"));

      Integer ifBlock = blockIndexes.get(ctx.block(0));
      programInstructionSet.addMultipleInstructions(tmp, ifBlock);

      elseBlockOffset = tmp.size();
      Integer elseBlock;
      try {
        elseBlock = blockIndexes.get(ctx.block(1)) + elseBlockOffset;
      } catch (NullPointerException npe) {
        elseBlock = blockIndexes_2.get(ctx.block(0)) + elseBlockOffset;
      }
      
      programInstructionSet.addInstruction(
        labelTuple("else_" + currentIfElseNumber + ":"), elseBlock);

      if (ctx.ELSE() != null) {
        // Jump to endif always comes just before the else block.
        programInstructionSet.addInstruction(
            jumpTuple("endif_" + currentIfElseNumber), elseBlock);
        programInstructionSet.addInstruction(labelTuple("endif_" + currentIfElseNumber + ":"));
      }
    }
    else if (ctx.methodCall() != null) {
      if (ctx.methodCall().CALLOUT() != null) handleCallout(ctx.methodCall());
    }
  }

  /**
   * Creates a new register for each variable, initialises variable to zero and adds both to map.
   * @param ctx The VarDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void enterVarDecl(DecafParser.VarDeclContext ctx) {
    ListIterator identifierListItr = ctx.IDENTIFIER().listIterator();
    while (identifierListItr.hasNext()) {
      String identifierName = ( (TerminalNode) identifierListItr.next() ).getText();
      String r0 = nextRegister();
      programInstructionSet.addInstruction(moveTuple("$0", r0)); // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< TODO: Potentially more efficient to use XOR with both operands as the same address than MOV
      variableRegisterMap.put(identifierName, r0);
    }
  }

  /**
   * Creates a new register for each global variable, initialises register to 0 and assigns the
   * location to the variable map.
   * For arrays, declares space in memory and adds a label to access it in other parts of the code.
   * @param ctx The FieldDeclContext object defined in DecafParser. Generated at compile time.
   */
  public void enterFieldDecl(DecafParser.FieldDeclContext ctx) {
    ListIterator identifierListItr = ctx.IDENTIFIER().listIterator();
    while (identifierListItr.hasNext()) {
      String identifierName = ( (TerminalNode) identifierListItr.next() ).getText();
      String r0 = nextRegister();
      programInstructionSet.addInstruction(moveTuple("$0", r0));
      variableRegisterMap.put(identifierName, r0);
    }

    ListIterator arrayidentifierListItr = ctx.arrayDecl().listIterator();
    while (arrayidentifierListItr.hasNext()) {
      DecafParser.ArrayDeclContext arrayDecl =
        (DecafParser.ArrayDeclContext) arrayidentifierListItr.next();      
      String identifierName = arrayDecl.IDENTIFIER().getText();    
      int arraySize = Integer.parseInt(arrayDecl.INTLITERAL().getText());
      dataSegment.add(arrayDeclTuple("array_" + identifierName + ":", arraySize));
    }
  }

  /**
   * Calculates the correct values of certain literals and adds those to the expr results map that
   * keeps track of expression evaluation results. I.e. if 'true' is written in the source code then
   * '1' is placed in a register and mapped to that token.
   * @param ctx The ExprContext object defined in DecafParser. Generated at compile time.
   */
  public void enterExpr(DecafParser.ExprContext ctx) {
    if (ctx.methodCall() != null) {
      if (ctx.methodCall().CALLOUT() != null) {
        handleCallout(ctx.methodCall());
        exprResultRegisterMap.put(ctx, "%rax");
      }
      else { 
        String r0 = nextRegister();
        programInstructionSet.addInstruction(
          callTuple(ctx.methodCall().methodName().IDENTIFIER().getText()));
        programInstructionSet.addInstruction(moveTuple("%rax", r0));
        exprResultRegisterMap.put(ctx, r0);
      }
    }
    else if (ctx.BOOLEANLITERAL() != null) {
      String v0 = getExprValue(ctx);
      String r0 = nextRegister();

      if      (v0.equals("true"))   programInstructionSet.addInstruction(moveTuple("$1", r0));
      else if (v0.equals("false"))  programInstructionSet.addInstruction(moveTuple("$0", r0));
        
      exprResultRegisterMap.put(ctx, r0);
    }
    else if (ctx.CHARLITERAL() != null) {
      String v0 = getExprValue(ctx);
      String r0 = nextRegister();
      int ascii = (int) v0.charAt(0);

      programInstructionSet.addInstruction(moveTuple("$" + Integer.toString(ascii), r0));
      exprResultRegisterMap.put(ctx, r0);
    }
  }

  /**
   * Adds the required sequence of ThreeCodeTuple objects to the InstructionSet that is necessary to
   * achieve the required arithmetic or boolean logic.
   * @param ctx The ExprContext object defined in DecafParser. Generated at compile time.
   */
  public void exitExpr(DecafParser.ExprContext ctx) {
    String v0 = getExprValue(ctx.expr(0));
    String v1 = getExprValue(ctx.expr(1));

    String r0 = nextRegister();
    String r1 = nextRegister();

    if (ExpressionOperationRules.arithmeticReturnsInteger(ctx)) {
      if (ctx.DIVISION() != null) {
        programInstructionSet.addInstruction(moveTuple(v0, "%rdx"));
        programInstructionSet.addInstruction(moveTuple("$0", "%rax"));
        programInstructionSet.addInstruction(divisionTuple(v1));
        programInstructionSet.addInstruction(moveTuple("%rax", r0));

        exprResultRegisterMap.put(ctx, r0);
      } else {
        programInstructionSet.addInstruction(moveTuple(v0, r0));
        programInstructionSet.addInstruction(moveTuple(v1, r1));
        
        if      (ctx.ADDITION() != null) 
          programInstructionSet.addInstruction(additionTuple(r0, r1));
        else if (ctx.MINUS()    != null)
          programInstructionSet.addInstruction(subtractionTuple(r0, r1));
        else if (ctx.MULTIPLY() != null)
          programInstructionSet.addInstruction(multiplicationTuple(r0, r1));

        exprResultRegisterMap.put(ctx, r1);
      }
    }
    else if (ExpressionOperationRules.booleanBinaryOperations(ctx)) {
      if (ExpressionOperationRules.equalityBinaryOperations(ctx)) {
        programInstructionSet.addInstruction(cmpTuple(v0, v1));

        if      (ctx.EQUAL()    != null) {
          programInstructionSet.addInstruction(moveEqualTuple("$1", r0));
          programInstructionSet.addInstruction(moveNotEqualTuple("$0", r0));
        }
        else if (ctx.NOTEQUAL() != null) {
          programInstructionSet.addInstruction(moveEqualTuple("$0", r0));
          programInstructionSet.addInstruction(moveNotEqualTuple("$1", r0));
        }
        exprResultRegisterMap.put(ctx, r0);
      }
      else if (ExpressionOperationRules.conditionalBinaryOperation(ctx)) {
        programInstructionSet.addInstruction(moveTuple(v0, r0));
        programInstructionSet.addInstruction(moveTuple(v1, r1));
        programInstructionSet.addInstruction(additionTuple(r0, r1));

        if      (ctx.AND()  != null) {
          // true bits are set to 1, so true && true = 1 + 1 would result in a 2.
          programInstructionSet.addInstruction(moveTuple("$2", r0));          
          programInstructionSet.addInstruction(cmpTuple(r1, r0));

          programInstructionSet.addInstruction(moveEqualTuple("$1", r0));
          programInstructionSet.addInstruction(moveNotEqualTuple("$0", r0));
        }
        else if (ctx.OR()   != null) {
          // false bits are set to 0, so any addition with true bit would result in a non-zero int.
          programInstructionSet.addInstruction(moveTuple("$0", r0));          
          programInstructionSet.addInstruction(cmpTuple(r1, r0));

          programInstructionSet.addInstruction(moveGreaterThanTuple("$1", r0));
          programInstructionSet.addInstruction(moveEqualTuple("$0", r0));
        }
        exprResultRegisterMap.put(ctx, r0);
      }
      else if (ExpressionOperationRules.arithmeticReturnsBoolean(ctx)) {
        programInstructionSet.addInstruction(cmpTuple(v0, v1));

        if      (ctx.LESSTHAN()    != null) {
          programInstructionSet.addInstruction(moveLessThanTuple("$1", r0));
          programInstructionSet.addInstruction(moveGreaterThanEqualTuple("$0", r0));
        }
        else if (ctx.GREATERTHAN() != null) {
          programInstructionSet.addInstruction(moveGreaterThanTuple("$1", r0));
          programInstructionSet.addInstruction(moveLessThanEqualTuple("$0", r0));
        }
        else if (ctx.LSSTHNEQTO()  != null) {
          programInstructionSet.addInstruction(moveLessThanEqualTuple("$1", r0));        
          programInstructionSet.addInstruction(moveGreaterThanTuple("$0", r0));
        }
        else if (ctx.GRTTHNEQTO()  != null) {
          programInstructionSet.addInstruction(moveGreaterThanEqualTuple("$1", r0));
          programInstructionSet.addInstruction(moveLessThanTuple("$0", r0));
        }
        exprResultRegisterMap.put(ctx, r0);
      }
    }
    else if (ctx.LPAREN() != null && ctx.RPAREN() != null) {
      String exprValue = getExprValue(ctx.expr(0));
      exprResultRegisterMap.put(ctx, exprValue);
    }
  }

  /**
   * Initialises the data region for the array declarations.
   * @param ctx The ProgramContext object defined in DecafParser. Generated at compile time.
   */
  public void enterProgram(DecafParser.ProgramContext ctx) {
    dataSegment.add(labelTuple(".data"));
  }


  /**
   * Adds all the contents of the data segment to the InstructionSet at the bottom so that they are
   * present but segregated from the main body of instructions.
   * @param ctx The ProgramContext object defined in DecafParser. Generated at compile time.
   */
  public void exitProgram(DecafParser.ProgramContext ctx) {
    // if the data segment has anything more than just it's label
    if (dataSegment.size() > 1) programInstructionSet.addMultipleInstructions(dataSegment, -1);
  }


  /**
   * Handles method calls to linked libraries.
   * @param ctx The MethodCallContext object defined in DecafParser. Generated at compile time.
   */
  public void handleCallout(DecafParser.MethodCallContext ctx) {
    String calloutName = ctx.STRINGLITERAL().getText();
    ListIterator calloutArgsItr = ctx.calloutArg().listIterator();        
    List<String> registerList =
      Arrays.asList(new String[]{"%rdi","%rsi","%rdx","%rcx","%r8","%r9"});
    ListIterator registerListItr = registerList.listIterator();

    while (calloutArgsItr.hasNext() && registerListItr.hasNext()) {
      DecafParser.CalloutArgContext arg = (DecafParser.CalloutArgContext) calloutArgsItr.next();
      if (arg.STRINGLITERAL() != null)
        programInstructionSet.addInstruction(moveTuple(
          arg.STRINGLITERAL().getText(), (String) registerListItr.next()));
      if (arg.expr()          != null) 
        programInstructionSet.addInstruction(moveTuple(
         getExprValue(arg.expr()), (String) registerListItr.next()));
    }

    // Remaining args pushed to stack.
    if (calloutArgsItr.hasNext()) {
      DecafParser.CalloutArgContext arg = (DecafParser.CalloutArgContext) calloutArgsItr.next();
      if (arg.STRINGLITERAL() != null)
        programInstructionSet.addInstruction(pushTuple(arg.STRINGLITERAL().getText()));
      if (arg.expr()          != null) 
        programInstructionSet.addInstruction(pushTuple(getExprValue(arg.expr())));
    }

    programInstructionSet.addInstruction(callTuple(calloutName));
  }

  /**
   *  Calculates the memory address related to the element of the array defined in the source code.
   *  Loads the base address into the base pointer register, subtracts 1 as arrays start at index 0,
   *  multiplies the value by 4 as 32-bit integers occupy 4 bytes of memory each, adds the offset 
   *  to the base address, and stores the final result back into the base pointer.
   *  @param  arrayName   The name of the array to access.
   *  @param  arrayIndex  The element of the array to access.
   */
  public void addArrayIndexAddressToPointer(String arrayName, String arrayIndex) {
    String r0 = nextRegister();

    programInstructionSet.addInstruction(loadEffectiveAddressTuple(arrayName));
    programInstructionSet.addInstruction(moveTuple(arrayIndex, r0));
    programInstructionSet.addInstruction(subtractionTuple("$1", r0));
    programInstructionSet.addInstruction(multiplicationTuple("$4", r0));
    programInstructionSet.addInstruction(additionTuple(r0, "%rbp"));
  }

  /**
   *  This method is responsible for generating the names of the registers to use in the IR. Whilst
   *  watching the following tutorial[1], I learned that in IR it is very common to use more
   *  registers than the machine architecture actually has. As this is common practice in industry,
   *  this design practice has been used here. In this way, a Decaf program can have an infinite
   *  number of registers. After this IR, optimisation is done to map many different variables down
   *  to the amount of registers actually available.
   *  @return String The name of the next register.
   */
  public String nextRegister() {
    String register = "";

    // If we are at the end of list ('z'); reset the list to the start and increment the counter.
    if (!charListItr.hasNext()) {
      charListItr = charList.listIterator(); 
      registerCounter++;
    }

    char currentChar = (char) charListItr.next();
    register = "%" + currentChar + Integer.toString(registerCounter);

    return register;
  }

  /**
   * Keeps track of the amount of IF statements in the source code so that they can be referenced
   * individually.
   * @return  String  The current value of the counter as a String.
   */
  public String nextIfLabelNumber() {
    ifLabelCounter++;
    return Integer.toString(ifLabelCounter);
  }

  /**
   * Keeps track of the amount of FOR LOOPS in the source code so that they can be referenced
   * individually.
   * @return  String  The current value of the counter as a String.
   */
  public String nextForLabelNumber() {
    forLabelCounter++;
    return Integer.toString(forLabelCounter);
  }

  /**
   *  An expression can have one of two possible values that are stored in different places:
   *  variableRegisterMap holds the register associated with a given location (variable),
   *  exprResultRegisterMap holds the register of the result of an expression (e.g. expr + expr),
   *  Main.exprValues is populated with the value of an literal int, boolean or char character.
   *  If an expression has been used and resides in a register, that register is returned. If not,
   *  the actual value from the source code is returned as a constant.
   *  @param  ctx     The context object of the expression that a value is needed for.
   *  @return String  Either a register if initialised or constant if not.
   */
  public String getExprValue(DecafParser.ExprContext ctx) {
    try {
      String locationName = ctx.location().IDENTIFIER().getText();
      return variableRegisterMap.get(locationName);
    } catch (NullPointerException npe) {  }

    String tmp_r0 = exprResultRegisterMap.get(ctx);
    String tmp_v  = Main.exprValues.get(ctx);

    if      (tmp_r0 != null)            return tmp_r0;
    else if (tmp_v  != null) {
      try                               { return "$" + Integer.parseInt(tmp_v); }
      catch (NumberFormatException nfe) { return tmp_v; }
    }
    else return null;
  }

  /**
   * Moves src to dest.
   * @return String A ThreeCodeTuple object representing a move command.
   */ 
  public ThreeCodeTuple moveTuple(String src, String dest) {
    return new ThreeCodeTuple("mov", src, dest);
  }

  /**
   * Moves src to dest if last comparison was equal.
   * @return String A ThreeCodeTuple object representing a move command.
   */ 
  public ThreeCodeTuple moveEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmove", src, dest);
  }

  /**
   * Moves src to dest if last comparison was not equal.
   * @return String A ThreeCodeTuple object representing a move command.
   */ 
  public ThreeCodeTuple moveNotEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovne", src, dest);
  }

  /**
   * Moves src to dest if last comparison was greater than equal.
   * @return String A ThreeCodeTuple object representing a move command.
   */ 
  public ThreeCodeTuple moveGreaterThanTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovg", src, dest);
  }

  /**
   * Moves src to dest if last comparison was less than equal.
   * @return String A ThreeCodeTuple object representing a move command.
   */ 
  public ThreeCodeTuple moveLessThanTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovl", src, dest);
  }

  /**
   * Moves src to dest if last comparison was greater than or equal.
   * @return String A ThreeCodeTuple object representing a move command.
   */ 
  public ThreeCodeTuple moveGreaterThanEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovge", src, dest);
  }

  /**
   * Moves src to dest if last comparison was less than or equal.
   * @return String A ThreeCodeTuple object representing a move command.
   */ 
  public ThreeCodeTuple moveLessThanEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovle", src, dest);
  }

  /**
   * Creates a procedure stack frame.
   * @return String A ThreeCodeTuple object representing an enter command.
   */ 
  public ThreeCodeTuple enterTuple(String src, String dest) {
    return new ThreeCodeTuple("enter", src, dest);
  }

  /**
   * Cleans up the local stack and resets the '%rsp' and '%rbp'.
   * @return String A ThreeCodeTuple object representing a leave command.
   */ 
  public ThreeCodeTuple leaveTuple() {
    return new ThreeCodeTuple("leave");
  }

  /**
   * Copies a value to the stack pointed at by '%rsp' to src and decreases '%rsp'.
   * @return String A ThreeCodeTuple object representing a push command.
   */ 
  public ThreeCodeTuple pushTuple(String src) {
    return new ThreeCodeTuple("push", src);
  }

  /**
   * Copies a value from the stack pointed at by '%rsp' to dest and increases '%rsp'.
   * @return String A ThreeCodeTuple object representing a pop command.
   */ 
  public ThreeCodeTuple popTuple(String dest) {
    return new ThreeCodeTuple("pop", dest);
  }

  /**
   * Calls a method or library function, passing control to the callee.
   * @return String A ThreeCodeTuple object representing a call command.
   */ 
  public ThreeCodeTuple callTuple(String target) {
    return new ThreeCodeTuple("call", target);
  }

  /**
   * Returns control from a method to the caller.
   * @return  String  A ThreeCodeTuple object representing a return command.
   */
  public ThreeCodeTuple returnTuple() {
    return new ThreeCodeTuple("ret");
  }

  /**
   * * Jumps to target unconditionally.
   * @return  String  A ThreeCodeTuple object representing a jump command.
   */
  public ThreeCodeTuple jumpTuple(String target) {
    return new ThreeCodeTuple("jmp", target);
  }

  /**
   * Jumps to target if the flag set from the last command is 1; equal.
   * @return  String  A ThreeCodeTuple object representing a jump command.
   */
  public ThreeCodeTuple jumpEqualTuple(String target) {
    return new ThreeCodeTuple("je", target);
  }

  /**
   * Jumps to target if the flag set from the last command is 0; not equal.
   * @return  String  A ThreeCodeTuple object representing a jump command.
   */
  public ThreeCodeTuple jumpNotEqualTuple(String target) {
    return new ThreeCodeTuple("jne", target);
  }

  /**
   * Compares two values and sets a flag depending on the result of src being greater than, equal to
   * or less than dest.
   * @return  String  A ThreeCodeTuple object representing a comparison command.
   */
  public ThreeCodeTuple cmpTuple(String src, String dest) {
    return new ThreeCodeTuple("cmp", src, dest);
  }
  
  /**
   * `add src, dest`: add src to dest and store result in dest.
   * @return  String  A ThreeCodeTuple object representing an addition command.
   */
  public ThreeCodeTuple additionTuple(String num0, String num1) {
    return new ThreeCodeTuple("add", num0, num1);
  }

  /**
   * `sub src, dest`: subtract source from dest and store result in dest.
   * @return  String  A ThreeCodeTuple object representing a label.
   */
  public ThreeCodeTuple subtractionTuple(String num0, String num1) {
    return new ThreeCodeTuple("sub", num0, num1);
  }

  /**
   * `imul src, dest`: multiply dest by source and store result in dest.
   *  @return  String  A ThreeCodeTuple object representing a multiplication command.
   */
  public ThreeCodeTuple multiplicationTuple(String num0, String num1) {
    return new ThreeCodeTuple("imul", num0, num1);
  }

  /**
   * `idiv divisor` Divide rdx:rax by divisor. Stores quotient in rax and store remain in rdx.
   *  @return  String  A ThreeCodeTuple object representing a division command.
   */
  public ThreeCodeTuple divisionTuple(String divisor) {
    return new ThreeCodeTuple("idiv", divisor);
  }

  /**
   * @return  String  A ThreeCodeTuple object representing a label.
   */
  public ThreeCodeTuple labelTuple(String label) {
    return new ThreeCodeTuple(label);
  }

  /**
   * Used to allocate the space in memory for the new array.
   * The .space directive allocates bytes; so the array size needs to be multiplied by 4 (32 bits).
   * Logically, a boolean element only needs 1 bit, however, on some architectures they fill 1 byte,
   * and as size shouldn't be an issue, I am going to leave this 4 bytes per element. Good place to
   * start for optimisation.
   * @param  name  The name of the new array.
   * @param  size  The size of the new array.
   */
  public ThreeCodeTuple arrayDeclTuple(String name, int size) {
    return new ThreeCodeTuple(name, ".space " + Integer.toString(size * 4));
  } 

  /**
   * Store in %rbp: the start address in memory for the provided label.
   * @param  label The name relating to the data in memory
   */
  public ThreeCodeTuple loadEffectiveAddressTuple(String label) {
    return new ThreeCodeTuple("lea", label, "%rbp");
  }

  /**
   * An object to encapsulate the information of a Three-code tuple that is made up of, at most, 
   * three individual parts. Constructor overloading allows either 1, 2 or 3 parts to be passed in
   * during construction.
   * Note, this class could be in it's own file, however, it wont be used by any other part of the
   * application so I think it is best practice to keep it with the code using it.
   */
  class ThreeCodeTuple {
    public ThreeCodeTuple(String command, String source, String destination) {
      this.command = command;
      this.source = source + ",";
      this.destination = destination;
    }

    public ThreeCodeTuple(String command, String source) {
      this.command = command;
      this.source = source;
      this.destination = "";
    }

    public ThreeCodeTuple(String command) {
      this.command = command;
      this.source = "";
      this.destination = "";
    }

    public final String command;
    public final String source;
    public final String destination;

    public String toString() {
      return command + " " + source + " " + destination; 
    }
  }

  /**
   * Class to wrap a List of ThreeCodeTuples. Indexes are manipulated to ensure that assembly code
   * is written in the correct sequence. The decision to use this over the Visitor pattern is due to
   * the simplicity of inserting/moving nodes in a list vs designing a complex tree traversal.
   * Note, this class could be in it's own file, however, it wont be used by any other part of the
   * application so I think it is best practice to keep it with the code using it.
   */
  public class InstructionSet {
    public InstructionSet() { addDefaultInstructions(); }
    public List<ThreeCodeTuple> instructions = new ArrayList<>();

    public void addInstruction(ThreeCodeTuple instruction) { instructions.add(instruction); }

    public void addInstruction(ThreeCodeTuple instruction, int index) { 
      instructions.add(index, instruction);
    }

    public void addMultipleInstructions(ArrayList<ThreeCodeTuple> instructionList, int index) {
      if (index == -1)  instructions.addAll(instructionList);
      else              instructions.addAll(index, instructionList);
    }

    public void removeLastInstructions(int amountToRemove) {
      instructions = instructions.subList(0, instructions.size() - amountToRemove);
    }

    public void addDefaultInstructions(){
      instructions.add(new ThreeCodeTuple(".global", "main"));
    }

    public String toString() {
      String string = "";
      for (ThreeCodeTuple instruction : instructions) { string += instruction.toString() + "\n"; }
      return string;
    }
  }
}