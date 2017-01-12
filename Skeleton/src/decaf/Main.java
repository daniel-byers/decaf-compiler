/**
 * @author Daniel Byers | 13121312
 */

package decaf;

import java.io.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import java6G6Z1010.tools.CLI.*;
import java.util.Hashtable;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.util.stream.*;
import java.util.*;

/**
 * Main compiler class. Contains all logic for scanning, parsing and compiling
 * Decaf source files.
 */
public class Main {
  // Declare class variables to hold important objects.
  private static DecafLexer lexer;
  private static Map<Integer, String> printableTypes;

  /**
   * Main entry point for the compiler. Controls flow of process through scan,
   * parse, and compile stages.
   * @param args  Supplied by command line and determined by user.
   */
  public static void main(String[] args) {
    // Call setup method. Used instead of constructor as command line arguments
    // are sent to the Main object's main() method, instead of passed into the
    // Main object upon construction.
    setUp(args);

    // If 'scan' or nothing was supplied as the parameter to -target 
    if (CLI.target == CLI.SCAN || CLI.target == CLI.DEFAULT) { scan(); }

    // If 'parse' was supplied as the parameter to -target
    else if (CLI.target == CLI.PARSE) { parse(); }
  }

  /**
  * Sets up CLI object that encapsulates the information required for the 
  * process of scanning input to be tokenised. This object also holds
  * information about command line arguments so that the necessary logic can be
  * written for them. inputStream is initialised to either the input from a
  * file or prompted from stdin. The ANTLRInputStream is instantiated from the
  * inputStream, and DecafLexar is instantiated from the ANTLRInputStream.
  * Finialising the setup, the printableTypes HashMap is populated with the 
  * types of tokens that are to be printed alongside the text.
  * @param args The command-line arguments supplied from main().
  */
  private static void setUp(String[] args) {
    try {
      // The first argument into parse is the arguments supplied from the
      // command line, the second is an empty array of optional arguments which
      // aren't used.
      CLI.parse(args, new String[0]); 
      
      InputStream inputStream = args.length == 0 ?
        System.in : new java.io.FileInputStream(CLI.infile);

      ANTLRInputStream antlrIOS = new ANTLRInputStream(inputStream);

      // This class file is generated at runtime by ANTLR.
      lexer = new DecafLexer(antlrIOS);

      // remove the console listener from the lexer
      lexer.removeErrorListeners();

      // Add custom ErrorListener to lexer
      lexer.addErrorListener(new SyntaxErrorListener(CLI.infile, CLI.outfile));

      // (ID, Name). Lookup done by Token type.
      printableTypes = new HashMap<Integer, String>() {{
        put(lexer.IDENTIFIER, "IDENTIFIER ");
        put(lexer.INTLITERAL, "INTLITERAL ");
        put(lexer.CHARLITERAL, "CHARLITERAL ");
        put(lexer.STRINGLITERAL, "STRINGLITERAL ");
        put(lexer.BOOLEANLITERAL, "BOOLEANLITERAL ");
      }};

      // Print out the HashMap if debugging enabled.
      if (CLI.debug) System.out.println("Printable types (ID,Name): " 
                                          + printableTypes.toString());

    } catch(Exception e) { System.out.println(CLI.infile + " " + e); }
  }

  /**
   * Iterates over the tokenised input building an iterable String object for
   * each token that can be written as a line of text to a file. This text is in
   * the form:   line number (optional)type text
   *
   */
  private static void scan() {
    try {
      // Print out the outfile name if debugging enabled.
      if (CLI.debug)
        System.out.println("Out-file: ../output/myoutput/" + CLI.outfile);

      // Token token2 = lexer.emit();
      // System.out.println("Token2: " + token2);
      // lexer inherites nextToken() from org.antlr.v4.runtime.TokenSource.
      // This provides the next Token(type, text, line, col) object from the
      // stream.
      for (Token token = lexer.nextToken(); token.getType() != Token.EOF; 
            token = lexer.nextToken()) {

        if (CLI.debug)
          System.out.println(
            token.getLine() + ":" + token.getCharPositionInLine() + "\t" +
            printableTypes.getOrDefault(token.getType(), "")
            + " " + token.getText() );

        // Build the token into a iterable list that can be written to a file.
        List<String> lines = Arrays.asList(token.getLine() + " " + 
          printableTypes.getOrDefault(token.getType(), "") +
          token.getText());

        // Requires an Iterable<> type object to write to File. Will not
        // accept a String alone.
        Files.write(
          Paths.get(CLI.outfile),
            lines, Charset.forName("UTF-8"),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      }
    // Print any errors to stdout.
    } catch(Exception e) { System.out.println(CLI.infile + " " + e); }
  }

  /**
   * Stage Two.
   */
  private static void parse() {
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    DecafParser parser = new DecafParser (tokens);
    ParseTree tree = parser.program();
    if (CLI.debug) {
      TreePrinterListener listener = new TreePrinterListener(parser);
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      String formatted = listener.toString();
      System.out.println(formatted);
    }
  }
}
