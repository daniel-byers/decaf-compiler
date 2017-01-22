/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

public class ArrayVariableSymbol extends VariableSymbol {
  public ArrayVariableSymbol(String name, Type type, int size) { 
    super(name, type);
    this.size = size;
  }
  
  int size;
}