/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

import java.util.*;

public abstract class BaseScope implements Scope {
  public BaseScope(Scope enclosingScope) { this.enclosingScope = enclosingScope;  }

  Scope enclosingScope;
  Map<String, Symbol> symbols = new LinkedHashMap<String, Symbol>();
  Map<String, Symbol> duplicateSymbols = new LinkedHashMap<>();

  /**
   * @param String The name of the Symbol to search for.
   * @return Returns a Symbol representing the argument, determined by name.
   */
  public Symbol resolve(String name) {
    Symbol symbol = symbols.get(name);
    if      (symbol != null)                return symbol;
    else if (getEnclosingScope() != null)   return getEnclosingScope().resolve(name);
    else                                    return null;
  }

  /**
   * Adds the Symbols to the scope or marks them as duplicated.
   * Also sets the scope on the Symbol so it can track where it belongs.
   * @param Symbol The Symbol of an argument passed into the method.
   */
  public void define(Symbol symbol) {
    if    (symbols.containsKey(symbol.name))  duplicateSymbols.put(symbol.name, symbol);
    else                                      symbols.put(symbol.name, symbol);
    symbol.scope = this;
  }

  public List<String> getDuplicates() { 
    List<String> newList = new ArrayList<>();
    newList.addAll(duplicateSymbols.keySet());
    return newList;
  }

  /**
   * @return The scope within which this Symbol resides.
   */
  public Scope getEnclosingScope() { return enclosingScope; }

  /**
   * @return A String representation of the Symbol in a printable format.
   */
  public String toString() { return getScopeName() + ":" + symbols.keySet().toString(); }

}