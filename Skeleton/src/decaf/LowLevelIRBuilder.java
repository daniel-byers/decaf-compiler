/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
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
    for(char alphabet = 'a'; alphabet <= 'z'; alphabet++) charList.add(alphabet); // <<<<<<<<<<<<<<< TODO: Find a better way to populate the character list. This is crazy.
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

  public void enterMethodDecl(DecafParser.MethodDeclContext ctx) { 
    programInstructionSet.addInstruction(labelTuple(ctx.methodName().IDENTIFIER().getText() + ":"));
  }

  public void exitMethodDecl(DecafParser.MethodDeclContext ctx) {
    if (!ctx.methodName().IDENTIFIER().getText().equals("main"))
      programInstructionSet.addInstruction(returnTuple());
  }

  public void enterBlock(DecafParser.BlockContext ctx) {
    blockIndexes.put(ctx, programInstructionSet.instructions.size());
  }
  
  /**
    * This method is required to calculate where to insert jump to else statements when there is no
    * else block.
    * @param  ctx The BlockContext 
    */
  public void exitBlock(DecafParser.BlockContext ctx) {
    blockIndexes_2.put(ctx, programInstructionSet.instructions.size());
  }

  public void enterStatement(DecafParser.StatementContext ctx) {
    if (ctx.FOR() != null) {
      String locationName = ctx.IDENTIFIER().getText();
      String r0 = nextRegister();
      variableRegisterMap.put(locationName, r0);
    }
  }

  public void exitStatement(DecafParser.StatementContext ctx) {
    if (ctx.assignOp() != null) {

      String locationName = ctx.location().IDENTIFIER().getText();
      String locationReg = variableRegisterMap.get(locationName);
      String v0 = getExprValue(ctx.expr(0));

      if      (ctx.assignOp().ASSIGNMENTP() != null) 
        programInstructionSet.addInstruction(additionTuple(v0, locationReg));
      else if (ctx.assignOp().ASSIGNMENTS() != null)
        programInstructionSet.addInstruction(subtractionTuple(v0, locationReg));
      else if (ctx.location().LBRACE() != null && ctx.location().RBRACE() != null) {
        String exprValue = getExprValue(ctx.location().expr()).substring(1);
        String r0 = variableRegisterMap.get(locationName + "_" + exprValue);
        programInstructionSet.addInstruction(moveTuple(v0, r0));
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
      
      // String currentForNumber = Integer.toString(forLabelCounter);
      String currentForNumber = nextForLabelNumber();
      int forBlock = blockIndexes.get(ctx.block(0));
      ArrayList<ThreeCodeTuple> tmp = new ArrayList<>();

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
      if (ctx.methodCall().CALLOUT() != null) {
        handleCallout(ctx.methodCall());
      }
    }
  }

  public void handleCallout(DecafParser.MethodCallContext ctx) {
    String calloutName = ctx.STRINGLITERAL().getText();
    ListIterator calloutArgsItr = ctx.calloutArg().listIterator();        
    List<String> registerList =
      Arrays.asList(new String[]{"%rdi","%rsi","%rdx","%rcx","%r8","%r9"});
    ListIterator registerListItr = registerList.listIterator();

    while (calloutArgsItr.hasNext()) {
      DecafParser.CalloutArgContext arg = (DecafParser.CalloutArgContext) calloutArgsItr.next();
      if (arg.STRINGLITERAL() != null)
        programInstructionSet.addInstruction(moveTuple(
          arg.STRINGLITERAL().getText(), (String) registerListItr.next()));
      if (arg.expr()          != null) 
        programInstructionSet.addInstruction(moveTuple(
         getExprValue(arg.expr()), (String) registerListItr.next()));
    }

    programInstructionSet.addInstruction(callTuple(calloutName));
  }  

  // public void handleCallout(DecafParser.MethodCallContext ctx) {
  //   String calloutName = ctx.methodCall().STRINGLITERAL().getText();
  //   ListIterator calloutArgsItr = ctx.methodCall().calloutArg().listIterator();        
  //   List<String> registerList =
  //     Arrays.asList(new String[]{"%rdi","%rsi","%rdx","%rcx","%r8","%r9"});
  //   ListIterator registerListItr = registerList.listIterator();

  //   while (calloutArgsItr.hasNext()) {
  //     DecafParser.CalloutArgContext arg = (DecafParser.CalloutArgContext) calloutArgsItr.next();
  //     if (arg.STRINGLITERAL() != null)
  //       programInstructionSet.addInstruction(moveTuple(
  //         arg.STRINGLITERAL().getText(), (String) registerListItr.next()));
  //     if (arg.expr()          != null) 
  //       programInstructionSet.addInstruction(moveTuple(
  //        getExprValue(arg.expr()), (String) registerListItr.next()));
  //   }

  //   programInstructionSet.addInstruction(callTuple(calloutName));
  // }

  public void enterVarDecl(DecafParser.VarDeclContext ctx) {
    ListIterator indentifierListItr = ctx.IDENTIFIER().listIterator();
    while (indentifierListItr.hasNext()) {
      String identifierName = ( (TerminalNode) indentifierListItr.next() ).getText();
      String r0 = nextRegister();
      programInstructionSet.addInstruction(moveTuple("$0", r0));
      variableRegisterMap.put(identifierName, r0);
    }
  }

  public void enterFieldDecl(DecafParser.FieldDeclContext ctx) {
    ListIterator indentifierListItr = ctx.IDENTIFIER().listIterator();
    while (indentifierListItr.hasNext()) {
      String identifierName = ( (TerminalNode) indentifierListItr.next() ).getText();
      String r0 = nextRegister();
      programInstructionSet.addInstruction(moveTuple("$0", r0));
      variableRegisterMap.put(identifierName, r0);
    }

    ListIterator arrayIndentifierListItr = ctx.arrayDecl().listIterator();
    while (arrayIndentifierListItr.hasNext()) {
      DecafParser.ArrayDeclContext arrayDecl =
        (DecafParser.ArrayDeclContext) arrayIndentifierListItr.next();      
      String identifierName = arrayDecl.IDENTIFIER().getText();    
      String r0 = nextRegister();
      programInstructionSet.addInstruction(moveTuple("$0", r0));
      variableRegisterMap.put(identifierName, r0);

      int arraySize = Integer.parseInt(arrayDecl.INTLITERAL().getText());
      for (int i = 0; i < arraySize; i++) {
        r0 = nextRegister();
        programInstructionSet.addInstruction(moveTuple("$0", r0));
        variableRegisterMap.put(identifierName + "_" + Integer.toString(i), r0);
      }
    }
  }

  // TODO:  Integer literals evaluate to their integer value. Character literals evaluate to their 
  //  --    integer ASCII values, e.g., ’A’ represents the integer 65. (The type of a character
  //  --    literal is int.)
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
  }

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
  }

  public void exitProgram(DecafParser.ProgramContext ctx) {
    // checks register name generation
    // for (int i = 0;i <= 260 ; i++ ) System.out.println(nextRegister());
  }

  /**
   *  This method is responsible for generating the names of the registers to use in the IR. Whilst
   *  watching the following tutorial (), I learned that in IR it is very common to use more
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

  public String nextIfLabelNumber() {
    ifLabelCounter++;
    return Integer.toString(ifLabelCounter);
  }

  public String nextForLabelNumber() {
    forLabelCounter++;
    return Integer.toString(forLabelCounter);
  }

  public String getExprValue(DecafParser.ExprContext ctx) {
    try { 
      String locationName = ctx.location().IDENTIFIER().getText();
      if (ctx.location().LBRACE() != null && ctx.location().RBRACE() != null) {
        String exprValue = getExprValue(ctx.location().expr()).substring(1);
        return variableRegisterMap.get(locationName + "_" + exprValue);
      }
      else return variableRegisterMap.get(locationName);
    } catch (NullPointerException npe) {}

    String tmp_r0 = exprResultRegisterMap.get(ctx);
    String tmp_v  = Main.exprValues.get(ctx);

    if      (tmp_r0 != null)            return tmp_r0;
    else if (tmp_v  != null) {
      try                               { return "$" + Integer.parseInt(tmp_v); }
      catch (NumberFormatException nfe) { return tmp_v; }
    }
    else return null;
  }

  public ThreeCodeTuple moveTuple(String src, String dest) {
    return new ThreeCodeTuple("mov", src, dest);
  }

  public ThreeCodeTuple moveEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmove", src, dest);
  }

  public ThreeCodeTuple moveNotEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovne", src, dest);
  }

  public ThreeCodeTuple moveGreaterThanTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovg", src, dest);
  }

  public ThreeCodeTuple moveLessThanTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovl", src, dest);
  }

  public ThreeCodeTuple moveGreaterThanEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovge", src, dest);
  }

  public ThreeCodeTuple moveLessThanEqualTuple(String src, String dest) {
    return new ThreeCodeTuple("cmovle", src, dest);
  }

  public ThreeCodeTuple enterTuple(String src, String dest) {
    return new ThreeCodeTuple("enter", src, dest);
  }

  public ThreeCodeTuple leaveTuple() {
    return new ThreeCodeTuple("leave");
  }

  public ThreeCodeTuple pushTuple(String src) {
    return new ThreeCodeTuple("push", src);
  }

  public ThreeCodeTuple popTuple(String src) {
    return new ThreeCodeTuple("push", src);
  }

  public ThreeCodeTuple callTuple(String target) {
    return new ThreeCodeTuple("call", target);
  }

  public ThreeCodeTuple returnTuple() {
    return new ThreeCodeTuple("ret");
  }

  public ThreeCodeTuple jumpTuple(String target) {
    return new ThreeCodeTuple("jmp", target);
  }

  public ThreeCodeTuple jumpEqualTuple(String target) {
    return new ThreeCodeTuple("je", target);
  }

  public ThreeCodeTuple jumpNotEqualTuple(String target) {
    return new ThreeCodeTuple("jne", target);
  }

  public ThreeCodeTuple cmpTuple(String src, String dest) {
    return new ThreeCodeTuple("cmp", src, dest);
  }
  
  // `add src, dest`: add src to dest.
  public ThreeCodeTuple additionTuple(String num0, String num1) {
    return new ThreeCodeTuple("add", num0, num1);
  }

  // `sub src, dest`: subtract source from dest
  public ThreeCodeTuple subtractionTuple(String num0, String num1) {
    return new ThreeCodeTuple("sub", num0, num1);
  }

  // `imul src, dest`: multiply dest by source
  public ThreeCodeTuple multiplicationTuple(String num0, String num1) {
    return new ThreeCodeTuple("imul", num0, num1);
  }

  // `idiv divisor` Divide rdx:rax by divisor. Store quotient in rax and store remain in rdx.
  public ThreeCodeTuple divisionTuple(String divisor) {
    return new ThreeCodeTuple("idiv", divisor);
  }

  public ThreeCodeTuple labelTuple(String label) {
    return new ThreeCodeTuple(label);
  }

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

  public class InstructionSet {
    public InstructionSet() { addDefaultInstructions(); }
    public List<ThreeCodeTuple> instructions = new ArrayList<>();

    public void addInstruction(ThreeCodeTuple instruction) { instructions.add(instruction); }

    public void addInstruction(ThreeCodeTuple instruction, int index) { 
      instructions.add(index, instruction);
    }

    public void addMultipleInstructions(ArrayList<ThreeCodeTuple> instructionList, int index) { 
      instructions.addAll(index, instructionList);
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