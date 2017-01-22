package decaf;

import org.antlr.v4.runtime.tree.*;

class ReferencePassListener extends DecafParserBaseListener {

  ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
  GlobalScope globalScope;
  Scope currentScope;

  public ReferencePassListener(GlobalScope globalScope, ParseTreeProperty<Scope> scopes) {
    this.globalScope = globalScope;
    this.scopes = scopes;
  } 

  
}