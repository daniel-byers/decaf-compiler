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
import java6G6Z1010.tools.CLI.*;

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
      
      int index = msg.indexOf(":");

      //Build the error into a iterable list that can be written to a file.
      List<String> lines = Arrays.asList(
        inFile.substring(inFile.lastIndexOf('/') + 1) + " line " + line
        + ":" + charPositionInLine + " unexpected char"
        + msg.substring(index));

      if (CLI.debug) System.out.println(lines.toString());

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