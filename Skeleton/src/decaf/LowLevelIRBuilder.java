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
    registerCounter = 0;
    charList = new ArrayList<>();
    // TODO: Find a better way to populate the character list. This is crazy.
    for(char alphabet = 'a'; alphabet <= 'z'; alphabet++) charList.add(alphabet);
    charListItr = charList.listIterator();
    variableRegisterMap = new HashMap<>();
    exprResultRegisterMap = new ParseTreeProperty<>();
  }

  private int registerCounter; 
  private List<Character> charList;
  private ListIterator charListItr;
  private Map<String, String> variableRegisterMap;
  private ParseTreeProperty<String> exprResultRegisterMap; 

  // This holds a list of ThreeCodeTuples in order that they are encounted in the tree, therefore,
  // also the order in which they appear in the Decaf source code. Each listener will be responsible
  // for adding however many ThreeCodeTuple objects to the InstructionSet that are required to do 
  // the task it's designed to do. After complete traversal of the ParseTree, a list of instructions
  // will represent the sequence of low level instructions to be generated.
  InstructionSet programInstructionSet = new InstructionSet();

  public void enterMethodDecl(DecafParser.MethodDeclContext ctx) { 

  }

  public void exitStatement(DecafParser.StatementContext ctx) {
    if (ctx.assignOp() != null) {
      String locationName = ctx.location().IDENTIFIER().getText();
      String locationReg = variableRegisterMap.get(locationName);
      String exprReg = exprResultRegisterMap.get(ctx.expr(0));

      programInstructionSet.addInstruction(moveTuple(exprReg, locationReg));
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
    }
  }
  // TODO:  Integer literals evaluate to their integer value. Character literals evaluate to their 
  //  --    integer ASCII values, e.g., ’A’ represents the integer 65. (The type of a character
  //  --    literal is int.)
  public void enterExpr(DecafParser.ExprContext ctx) {
    if (ctx.ADDITION() != null) {
      String r0 = nextRegister();
      String r1 = nextRegister();
      programInstructionSet.addInstruction(moveTuple("$" + ctx.expr(0).INTLITERAL().getText(), r0));
      programInstructionSet.addInstruction(moveTuple("$" + ctx.expr(1).INTLITERAL().getText(), r1));
      programInstructionSet.addInstruction(additionTuple(r0, r1));
      exprResultRegisterMap.put(ctx, r1);
    }
    else if (ctx)
  }

  public void exitProgram(DecafParser.ProgramContext ctx) { 
    System.out.println(programInstructionSet.toString());

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

    // If we are at the end of list ('z'); reset the list to the start and reset the counter to 0.
    if (!charListItr.hasNext()) {
      charListItr = charList.listIterator(); 
      registerCounter++;
    }

    char currentChar = (char) charListItr.next();
    register = "%" + currentChar + Integer.toString(registerCounter);

    return register;
  }

  public ThreeCodeTuple moveTuple(String src, String dest) {
    return new ThreeCodeTuple("mov", src, dest);
  }

  public ThreeCodeTuple cmoveTuple(String src, String dest) {
    return new ThreeCodeTuple("cmov", src, dest);
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

  public ThreeCodeTuple jumpEqTuple(String target) {
    return new ThreeCodeTuple("je", target);
  }

  public ThreeCodeTuple jumpNEqTuple(String target) {
    return new ThreeCodeTuple("jne", target);
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

  // 

  class ThreeCodeTuple {
    public ThreeCodeTuple(String command, String source, String destination) {
      this.command = command;
      this.source = source;
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

    public void addDefaultInstructions(){
      instructions.add(new ThreeCodeTuple(".global", "main"));
      instructions.add(new ThreeCodeTuple("main:"));
    }

    public String toString() {
      String string = "";
      for (ThreeCodeTuple instruction : instructions) { string += instruction.toString() + "\n"; }
      return string;
    }
  }
}