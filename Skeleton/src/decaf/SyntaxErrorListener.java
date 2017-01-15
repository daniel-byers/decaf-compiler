/**
 * @author Daniel Byers | 13121312
 */

package decaf;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.Token;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.util.stream.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;

class SyntaxErrorListener extends BaseErrorListener {

    private static String inFile;
    private static String outFile;

    public SyntaxErrorListener(String inFile, String outFile){
      this.inFile = inFile;
      this.outFile = outFile;
    }

    @Override
    public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol,
         int line, int charPositionInLine, String msg, RecognitionException e) {
      
      // System.out.println();
      // System.out.println("inFile: " + inFile);

      // System.out.println("Offending Symbol: " + offendingSymbol);
      // System.out.println("Line: " + line);
      // System.out.println("Char Position in Line: " + charPositionInLine);
      // System.out.println("Message: " + msg);

      // System.out.println("-- Recognizer<?,?> --");
      // DecafLexer lexer = (DecafLexer) recognizer;
      // System.out.println(recognizer);
      // System.out.println("Parse/Lex Info: " + lexer.getParseInfo());
      // System.out.println("Vocabulary: " + lexer.getVocabulary());
      // System.out.println("Char index: " + lexer.getCharIndex());
      // System.out.println("Char in line: " + lexer.getCharPositionInLine());
      // System.out.println("Line: " + lexer.getLine());
      // System.out.println("Token : " + lexer.getToken());
      // System.out.println("Type: " + lexer.getType());
      // System.out.println("Text: " + lexer.getText());

      // System.out.println("-- RecognitionException --");
      // System.out.println(e);
      // System.out.println("Context: " + e.getCtx());
      // // System.out.println("Expected Tokens: " + e.getExpectedTokens());
      // System.out.println("Input Stream: " + e.getInputStream());
      // System.out.println("Offending State: " + e.getOffendingState());
      // System.out.println("Offending Token: " + e.getOffendingToken());
      // System.out.println("Recognizer: " + e.getRecognizer());


      int index = msg.indexOf(":");
      // String illegalToken = msg.substring(index + 3);
      // System.out.println(
      //   illegalToken.substring(illegalToken.indexOf("'"),
      //                          illegalToken.lastIndexOf("'")));

      // System.out.println("0x" + String.format("%01x", (int) '\n').toUpperCase());

      //Build the error into a iterable list that can be written to a file.
      List<String> lines = Arrays.asList(
        inFile.substring(inFile.lastIndexOf('/') + 1) + " line " + line
        + ":" + charPositionInLine + " unexpected char"
        + msg.substring(index));

      try {
        // Requires an Iterable<> type object to write to File. Will not
        // accept a String alone.
        Files.write(
          Paths.get(outFile),
            lines, Charset.forName("UTF-8"),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch(Exception e2) { System.out.println(outFile + " " + e2); }
    }
}