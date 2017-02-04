/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

import org.antlr.v4.runtime.Token;
import java6G6Z1010.tools.CLI.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class ErrorHandler {
  public ErrorHandler() {
    try {
      infile = Files.readAllLines(Paths.get(CLI.infile));
      errors = new HashMap<>();
    } catch(Exception e) { System.out.println("I/O Error: " + e); }
  }

  List<String> infile;
  Map<Token, Error> errors;

  public int totalErrors() { return errors.size(); }

  public void handleError(String message, Token offendingToken) {
    errors.put(
      offendingToken,
      _buildError(message, offendingToken.getLine(), offendingToken.getCharPositionInLine())
    );
  }

  // Create an empty string with the amount of whitespace reflecting the distance between the left
  // margin and the location of the invalid Symbol.
  private Error _buildError(String message, int lineNumber, int charPos) {
    String fileNameAndLineNumber = 
      Paths.get(CLI.infile).toAbsolutePath() + ":" + Integer.toString(lineNumber + 1) + "\n";
    String strPointer = new String(new char[charPos]).replace("\0", " ") + "^" + "\n";
    String line = infile.get(lineNumber - 1) + "\n"; 
    String errorMessage = "error: " + message + "\n";

    return new Error(fileNameAndLineNumber, strPointer, line, errorMessage);
  }

  public void printErrors() {
    // TODO: Remove duplicate errors from HashMap
    _printErrors();
    System.out.println("[decaf] TOTAL ERRORS: " + totalErrors());
  }

  private void _printErrors() {for (Error error : errors.values()) System.out.println(error); }

  class Error {
    public Error(String fileNameAndLineNumber, String pointer, String line, String errorMessage) {
      this.fileNameAndLineNumber = fileNameAndLineNumber;
      this.pointer = pointer;
      this.line = line;
      this.errorMessage = errorMessage;
    }

    String fileNameAndLineNumber;
    String pointer;
    String line;
    String errorMessage;

    public String toString() {
      return String.join("[decaf]\t", "", fileNameAndLineNumber, errorMessage, line, pointer);
    }
  } 
}
