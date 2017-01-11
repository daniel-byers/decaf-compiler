package decaf;

import java.io.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import java6G6Z1010.tools.CLI.*;
import java.util.Hashtable;
import java.nio.file.*;
import java.util.stream.*;

public class Main { //implements TokenSelector {
  public static void main(String[] args) {
    try {
      // Sets up CLI object that encapsulates the information required for the process of scanning
      // input to be tokenised. This object also holds information about command line arguments so that
      // the necessary logic can be written for them. The first argument into parse is the arguments
      // supplied from the command line, the second is an empty array of optional arguments which aren't
      // used.
      CLI.parse(args, new String[0]); 
      
      // If there were arguments, open the file specified, otherwise enter interactive mode via stdin.
      InputStream inputStream = args.length == 0 ? System.in : new java.io.FileInputStream(CLI.infile);

      // Instantiate the ANTLR input stream from the specified location.
      ANTLRInputStream antlrIOS = new ANTLRInputStream(inputStream);

      // IF 'scan' or nothing was supplied as the parameter to -target 
      if (CLI.target == CLI.SCAN || CLI.target == CLI.DEFAULT) {

        // Instantiate a lexer determined by the DecafLexer.g4. This file is generated at runtime by ANTLR.
        DecafLexer lexer = new DecafLexer(antlrIOS);

        // Load DecafLexer tokens into hashtable. This is done by finding the index of the '=' in the line
        // and adding the number that follows the '=' as the key to the hashtable, and everything that 
        // preceeds the '=' is set as the name. It's worth noting the reason String.split('=') wasn't used
        // here is due to equals signs (=) showing up in the names of classes and therefore splits on the
        // wrong character.
        Hashtable<Integer, String> token_ids = new Hashtable<Integer, String>();
        try (Stream<String> stream = Files.lines(Paths.get("src/decaf/DecafLexer.tokens"))) {
                stream.forEach(
                  (x) -> {
                    int equalsLocation = x.lastIndexOf("=");
                    token_ids.put(Integer.parseInt(x.substring(equalsLocation + 1)), x.substring(0, equalsLocation));
                  }
                );
        }

        System.out.println(token_ids.toString());

        // From the org.antlr.v4.runtime.Token library; Token consists of:
        // type: the integer type of the token
        // text: the text of the token
        // line: the line in which the token appears
        // col:  the column in which the token appears
        Token token;

        // stop condition
        boolean done = false;
        while (!done) {
          try {

            // lexer inherites nextToken() from org.antlr.v4.runtime.TokenSource. This provides the next 
            // Token object from the stream.
            for (token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {
              // DecafLexer.ID is a constant set to the id corresponding to the number in which the
              // rule was defined; ID was the fourth rule defined so if the type of token equals four
              // then "INDENTIFIER" is concatenated to the string, else it's not.
              System.out.println(
                token.getLine() + ":" + token.getCharPositionInLine() + "\t" +
                token_ids.get(token.getType()) + " " + token.getText() );
            
            }
            // apply stop condition
            done = true;

            // print any errors to stdout.
          } catch(Exception e) { System.out.println(CLI.infile + " " + e); }
        }
      }



      // STAGE 2
      else if (CLI.target == CLI.PARSE) {
        DecafLexer lexer = new DecafLexer(antlrIOS);
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
    } catch(Exception e) { System.out.println(CLI.infile+" "+e); }
  }
}
