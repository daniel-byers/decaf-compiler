/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

public class LocalScope extends BaseScope {
  public LocalScope(Scope parent) { super(parent); }
  
  /**
   * @return The name of this Symbol.
  */
  public String getScopeName() { return "locals"; }
}