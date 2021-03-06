/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

import java.io.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import java6G6Z1010.tools.CLI.*;
import java.util.Hashtable;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.util.stream.*;
import java.util.*;
import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.JFrame;

/**
 * Main compiler class. Contains all logic for scanning, parsing and compiling Decaf source files.
 */
public class Main {
  /**
   * Empty constructor for default usage.
   */
  public Main () {}

  /**
   * Constructor used for adding extra listeners. Will help later in testing as we can inject a
   * listener that doesn't write out to a file or to stdout; removing the need for I/O. Not used.
   * @param listeners Array of listeners to add to the Lexer and Parser.
   */
  public Main(ANTLRErrorListener[] listeners) { _extraErrorListeners = listeners; }

  // Declare class variables to hold important objects.
  private static DecafLexer lexer;
  private static DecafParser parser;
  private static Map<Integer, String> printableTypes;
  private static ANTLRErrorListener[] _extraErrorListeners;
  private static ErrorHandler errorHandler;
  public static ParseTreeProperty<String> exprValues = new ParseTreeProperty<>();

  /**
   * Main entry point for the compiler. Controls flow of process through scan, parse, and compile
   * stages.
   * @param args  Supplied by command line and determined by user.
   */
  public static void main(String[] args) {
    // Call setup method. Used in lieu of constructor as command line arguments are sent to the Main
    // object's main() method, instead of passed into the Main object upon construction.
    setUp(args);

    if      (  CLI.target == CLI.DEFAULT
            || CLI.target == CLI.SCAN     )  scan();
    else if (  CLI.target == CLI.PARSE    )  parse();
    else if (  CLI.target == CLI.INTER    )  check(parse());
    else if (  CLI.target == CLI.ASSEMBLY )  { 
      ParseTree tree = parse();
      if    (check(tree)) generateCode(tree);
      else  System.out.println("[decaf] BUILD FAILED.");
    }
  }

  protected static void setUp(String[] args) { setUp(args, false); }

  /**
  * Sets up the CLI object that encapsulates the information captured from the command line, the
  * DecafLexar that is instantiated from the input stream, and the DecafParser which is in turn
  * instantiated with the tokens output from the Lexer.
  * The extra error listeners that were injected during construction of this object are registered
  * with the Lexer and Parser.
  * @param args The command-line arguments supplied from main().
  */
  protected static void setUp(String[] args, boolean stringInput) {
    try {
 
      InputStream inputStream = null;
      if (stringInput) inputStream = new ByteArrayInputStream(args[0].getBytes());
      else {
        // The first argument into parse is the arguments supplied from the command line, the second
        // is an empty array of optional arguments which aren't used.

        CLI.parse(args, new String[0]);

        if    (args.length == 0)  inputStream = System.in; 
        else                      inputStream = new java.io.FileInputStream(CLI.infile);
      }

      ANTLRInputStream antlrIOS = new ANTLRInputStream(inputStream);

      lexer = new DecafLexer(antlrIOS);

      CommonTokenStream tokens = new CommonTokenStream(lexer);

      parser = new DecafParser(tokens);

      // Remove the ConsoleListener from the Lexer and add custom ErrorListener.
      lexer.removeErrorListeners();
      lexer.addErrorListener(new SyntaxErrorListener(CLI.infile, CLI.outfile));

      // Add custom ErrorHandler to print better error messages and reduce duplication.
      errorHandler = new ErrorHandler();

      // Add DiagnosticErrorListener to Parser and set ambiguity reporting to high.
      // TODO: We want to fail build and print grammatical errors encountered during parsing
      if (CLI.debug) {
        parser.addErrorListener(new DiagnosticErrorListener());
        parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
      }

      // Add all custom error listeners to the Lexer and Parser.
      if (_extraErrorListeners != null) {
        for(ANTLRErrorListener listener : _extraErrorListeners) {
          lexer.addErrorListener(listener);
          parser.addErrorListener(listener);
        }
      }

      // (ID, Name). Select types of tokens that are to be printed alongside the text during error
      // reporting. Lookup done by Token type.
      printableTypes = new HashMap<Integer, String>() {{
        put(lexer.IDENTIFIER, "IDENTIFIER ");
        put(lexer.INTLITERAL, "INTLITERAL ");
        put(lexer.CHARLITERAL, "CHARLITERAL ");
        put(lexer.STRINGLITERAL, "STRINGLITERAL ");
        put(lexer.BOOLEANLITERAL, "BOOLEANLITERAL ");
      }};

      if (CLI.debug) { 
        System.out.println("In file: " + CLI.infile);
        System.out.println("Out file: " + CLI.outfile);
      }

    } catch(Exception e) { System.out.println(CLI.infile + " " + e); }
  }

  /**
   * Iterates over the tokenised input building an iterable String object for each token that can be
   * written as a line of text to a file. This text is in the form: line number (optional)type text
   */
  protected static void scan() {
    if (CLI.debug)  System.out.println("Printable types (ID,Name): " + printableTypes.toString());
    // Lexer provides the next Token(type, text, line, col) object from the stream.
    for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {

      if (CLI.debug)
        System.out.println(token.getLine() + ":" + token.getCharPositionInLine() + "\t" +
          printableTypes.getOrDefault(token.getType(), "") + " " + token.getText());

      printToFile(Arrays.asList(token.getLine() + " " + 
        printableTypes.getOrDefault(token.getType(), "") + token.getText()));
    }
  }

  /**
   * Builds a ParseTree from the tokens scanned by the Lexer.
   * @return ParseTree The Abstract Syntax Tree (AST) generated from the tokens.
   */
  protected static ParseTree parse() {
    // Returns the Context object for "program" (defined in DecafParser.g4)
    ParseTree tree = parser.program();
    TreePrinterListener listener = new TreePrinterListener(parser);
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    
    if (CLI.target == CLI.PARSE) printToFile(Arrays.asList(listener.toString()));

    if (CLI.debug) {
      System.out.println(listener.toString());      
      showTree(tree);
    }

    return tree;
  }

  /**
   * @param ParseTree The AST generated by the #parse method.
   */
  protected static void generateCode(ParseTree tree) {
    buildLowLevelIR(tree);
    allocateRegisters();
  }

  /** 
   * This is where design choices come in as there are many designs available at this point. Stack
   * of scopes, listeners, and the visitor pattern are all viable choices. Listeners automatically
   * visit each node and there is a Listener interface generated by ANTLR to contain all the enter
   * and exit Rule methods required by a listener. In addition, there is also a base listener
   * generated that implements all the rules in the interface with default functionality (they are
   * empty methods). Listeners are good, but they don't allow us to control the walk of the tree, to 
   * achieve this, you would use the visitor pattern. 
   * Traverses the tree to define all the Symbols and ensure they are valid. Symbol table will be
   * implemented as a logical stack of scopes where each scope contains a pointer to it's parent.
   * The reason I have decided to use listeners is because so much effort has been put into making
   * ANTLR robust, and so it feels foolish (not to mention it's extremely bad practice) to rewrite
   * boilerplate code when it's already been implemented in an intelligent fashion. Also, the walk
   * of the tree doesn't need to be controlled specifically so a listener is perfect.
   * @param   ParseTree The AST generated by the #parse method.
   * @return  boolean   Returns true if no semantic errors encountered, false otherwise.
   */
  protected static boolean check(ParseTree tree) {
    SemanticRuleManager manager = new SemanticRuleManager(errorHandler);
    ParseTreeWalker.DEFAULT.walk(manager, tree);

    if (errorHandler.totalErrors() > 0) errorHandler.printErrors();

    return errorHandler.totalErrors() == 0;
  }

  /**
   * Builds a instruction set that contains a list of assembly language instructions based on the
   * code written in the Decaf source code. This intermediate representation is not valid x86_64
   * assembly as it uses an unlimited amount of pseudo-registers.
   * @param tree  The AST built during the parse of the Decaf source code.
   */
  private static void buildLowLevelIR(ParseTree tree) {
    LowLevelIRBuilder builder = new LowLevelIRBuilder();
    ParseTreeWalker.DEFAULT.walk(builder, tree);

    if (CLI.target == CLI.ASSEMBLY)
      printToFile(Arrays.asList(builder.programInstructionSet.toString()));
    
    if (CLI.debug) System.out.println(builder.programInstructionSet.toString());
  }

  /**
   * Next stage of the compiler is to take all the temporaries created during low level IR buidling
   * and map these temporaries to registers in such a way that the same register doesn't hold two
   * temporaries that are live at the same time. First a liveness graph is created, then once that
   * has been defined a Register Interference Graph (RIG) is drawn to show which temporaries can
   * share a register. Finally, graph colouring is used to assign the registers.
   */
  private static void allocateRegisters() {}

  /**
   * Creates a Swing frame object and prints the AST in a graphical form. Useful for debugging.
   * @param tree  The AST built during the parse of the Decaf source code.
   */
  private static void showTree(ParseTree tree) {
    JFrame frame = new JFrame("Tree");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    JPanel container = new JPanel();
    JScrollPane scrPane = new JScrollPane(container);

    frame.getContentPane().add(scrPane);

    TreeViewer treeViewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
    treeViewer.setScale(3);
    container.add(treeViewer);

    frame.pack();
    frame.setVisible(true);
  }

  /**
   * Requires an Iterable<T> type object to write to File. Will not accept a String alone.
   * @param lines List of lines to be written to the file.
   */
  private static void printToFile(List<String> lines) {
    try {
      Files.write(Paths.get(CLI.outfile), lines, Charset.forName("UTF-8"),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch(Exception e) { System.out.println("I/O Error: " + e); }
  }
}
