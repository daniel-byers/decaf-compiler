/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

package decaf;

public interface Scope {
    public String getScopeName();
    public Scope getEnclosingScope();
    public void define(Symbol sym);
    public Symbol resolve(String name);
}