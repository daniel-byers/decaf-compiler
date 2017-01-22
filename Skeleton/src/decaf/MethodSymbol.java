/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

import java.util.LinkedHashMap;
import java.util.Map;

public class MethodSymbol extends Symbol implements Scope {
  public MethodSymbol(String name, Type returnType, Scope enclosingScope) {
    super(name, returnType);
    this.enclosingScope = enclosingScope;
  }

  Map<String, Symbol> formalParameters = new LinkedHashMap<String, Symbol>();
  Scope enclosingScope;

  /**
   *
   * @return Returns a Symbol representing the argument, determined by name.
   */
  public Symbol resolve(String name) {
    Symbol symbol = formalParameters.get(name);
    if      (symbol != null)                return symbol;
    else if (getEnclosingScope() != null)   return getEnclosingScope().resolve(name);
    else                                    return null;
  }

  /**
   * Adds the arguments to the scope, and sets the scope on the argument symbol.
   * @param Symbol The Symbol of an argument passed into the method.
   */
  public void define(Symbol symbol) {
    formalParameters.put(symbol.name, symbol);
    symbol.scope = this;
  }

  /**
   * @return The scope within which this Symbol resides.
   */
  public Scope getEnclosingScope() { return enclosingScope; }

  /**
   * @return The name of this Symbol.
   */
  public String getScopeName() { return name; }

  /**
   * @return A String representation of the Symbol in a printable format.
   */
  public String toString() {
    return "Method:" + super.toString() + "|parameters: " + formalParameters.values();
  }
}