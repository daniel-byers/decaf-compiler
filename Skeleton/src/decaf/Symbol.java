/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

public class Symbol {
  public Symbol(String name) { this.name = name; }
  public Symbol(String name, Type type) { this(name); this.type = type; }

  /**
   * Define the types of Symbols that are available in Decaf.
   */
  public static enum Type { VOID, INT, BOOLEAN }

  String name;
  Type type;
  Scope scope;
  
  /**
   * @return A String representation of the symbol's name.
  */
  public String getName() { return name; }

  /**
   * @return A String representation of the symbol in a printable format.
   */
  public String toString() { return getName() + ":" + type; }

  /**
   * @param int Integer of the Token's type.
   * @return Returns the type relating to the symbol, determined by matching token and parse types.
   */
  public static Symbol.Type getType(int tokenType) {
    switch (tokenType) {
      case DecafParser.VOID    : return Type.VOID;
      case DecafParser.INT     : return Type.INT;
      case DecafParser.BOOLEAN : return Type.BOOLEAN;
      default                  : return null;
    }
  }
}