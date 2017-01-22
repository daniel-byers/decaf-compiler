/**
 * @author Daniel Byers | 13121312
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
  // Declare class variables to hold important objects.
  private static DecafLexer lexer;
  private static DecafParser parser;
  private static Map<Integer, String> printableTypes;
  private static ANTLRErrorListener[] _extraErrorListeners;

  /**
   * Empty constructor for default usage.
   */
  public Main () {}

  /**
   * Constructor used for adding extra listeners. Will help later in testing as we can inject a
   * listener that doesn't write out to a file or to stdout; removing the need for I/O.
   * @param listeners Array of listeners to add to the Lexer and Parser.
   */
  public Main(ANTLRErrorListener[] listeners) { _extraErrorListeners = listeners; }

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
            || CLI.target == CLI.SCAN   )  scan();
    else if (  CLI.target == CLI.PARSE  )  parse();
    else if (  CLI.target == CLI.INTER  )  check(parse());
  }

  /**
  * Sets up the CLI object that encapsulates the information captured from the command line, the
  * DecafLexar that is instantiated from the input stream, and the DecafParser which is in turn
  * instantiated with the tokens output from the Lexer.
  * The extra error listeners that were injected during construction of this object are registered
  * with the Lexer and Parser.
  * @param args The command-line arguments supplied from main().
  */
  private static void setUp(String[] args) {
    try {
      // The first argument into parse is the arguments supplied from the command line, the second
      // is an empty array of optional arguments which aren't used.
      CLI.parse(args, new String[0]);
 
      InputStream inputStream = args.length == 0 ?
        System.in : new java.io.FileInputStream(CLI.infile);

      // Retrieve input from whatever source was decided.
      ANTLRInputStream antlrIOS = new ANTLRInputStream(inputStream);

      // This class file is generated at runtime by ANTLR.
      lexer = new DecafLexer(antlrIOS);

      // Retrieve tokens from Lexer.
      CommonTokenStream tokens = new CommonTokenStream(lexer);

      // // This class file is generated at runtime by ANTLR.
      parser = new DecafParser(tokens);

      // Remove the ConsoleListener from the Lexer and add custom ErrorListener.
      lexer.removeErrorListeners();
      lexer.addErrorListener(new SyntaxErrorListener(CLI.infile, CLI.outfile));

      // Add DiagnosticErrorListener to Parser and set ambiguity reporting to high.
      parser.addErrorListener(new DiagnosticErrorListener());
      parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

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
        put(lexer.NOT, "LOGICNOT ");
      }};

      if (CLI.debug) { 
        System.out.println("In file: " + CLI.infile);
        System.out.println("Out file: " + CLI.outfile);
        System.out.println("Printable types (ID,Name): " + printableTypes.toString());
      }

    } catch(Exception e) { System.out.println(CLI.infile + " " + e); }
  }

  /**
   * Iterates over the tokenised input building an iterable String object for each token that can be
   * written as a line of text to a file. This text is in the form: line number (optional)type text
   */
  private static void scan() {
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
   * @return ParseTree The Abstract Parse Tree (APT) generated from the tokens.
   */
  private static ParseTree parse() {
    // Returns the Context object for "program" (defined in DecafParser.g4)
    ParseTree tree = parser.program();

    // Creates the object that will listen as the tree is traversed.
    TreePrinterListener listener = new TreePrinterListener(parser);

    // Walks the tree, building the string representation of it.
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    
    printToFile(Arrays.asList(listener.toString()));

    if (CLI.debug) {
      System.out.println(listener.toString());
      JFrame frame = new JFrame("Tree");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      TreeViewer treeViewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
      treeViewer.setScale(5);
      frame.getContentPane().add(treeViewer);
      frame.pack();
      frame.setVisible(true);
    }

    return tree;
  }

  // This is where design choices come in as there are many designs available at this point. Stack
  // of scopes, listeners, and the visitor pattern are all viable choices. Listeners automatically
  // visit each node and there is a Listener interface generated by ANTLR that contains all the enter
  // and exit Rule methods required by a listener. In addition, there is also a base listener generated
  // the implements all the rules in the interface with default functionality (they are empty methods).
  // Listeners are good, but they don't allow us to control the walk of the tree, to do this, you would
  // use the visitor pattern.
  // Also, could use tree annotation.
  // Two phases, first we define all the symbols, then we make sure they exist. This allows forward
  // reference. Symbol table will be implemented as a stack of scopes controlled by listeners.
  // The reason I have decided to use listeners is because so much effort has been put into making
  // ANTLR robust, and so it feels foolish to rewrite boilerplate code when it's already been done the
  // "best" way possible.
  // Enter Rule: push scope
  // Exit Rule: pop scope
  // Parse tree:
  // Terminals at the leaves, non terminals at the interior nodes
  // In order traversal of the tree is the original input
  // Shows associations of operations that the input string doesn't.
  private static void check(ParseTree tree) {
    DefinitionPassListener definitionPass = new DefinitionPassListener();
    ParseTreeWalker.DEFAULT.walk(definitionPass, tree);

    ReferencePassListener referencePass =
      new ReferencePassListener(definitionPass.globalScope, definitionPass.scopes);
    ParseTreeWalker.DEFAULT.walk(referencePass, tree);
  }

  /**
   * Requires an Iterable<> type object to write to File. Will not accept a String alone.
   * @param lines List of lines to be written to the file.
   */
  private static void printToFile(List<String> lines) {
    try {
      Files.write(Paths.get(CLI.outfile), lines, Charset.forName("UTF-8"),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch(Exception e) { System.out.println("I/O Error: " + e); }
  }
}
