
parser grammar DecafParser;
options { tokenVocab = DecafLexer; }


program: CLASS IDENTIFIER LCURLY fieldDecl* methodDecl* RCURLY EOF;

fieldDecl: type ((IDENTIFIER | IDENTIFIER LBRACE INTLITERAL RBRACE) COMMA?)+ EOL;

methodDecl:
  (type | VOID) methodName LPAREN ((type IDENTIFIER COMMA?)+)? RPAREN block;

// Variables have to be declared first?
block: LCURLY varDecl* statement* RCURLY;

varDecl: type (IDENTIFIER COMMA?)+ EOL;

type: (INT | BOOLEAN);

statement : 
          ( location assignOp expr EOL
          | methodCall EOL
          | IF LPAREN expr RPAREN block (ELSE block)?
          | FOR IDENTIFIER ASSIGNMENT expr COMMA expr block
          | RETURN expr? EOL
          | BREAK EOL
          | CONTINUE EOL
          | block
          );

assignOp: (ASSIGNMENT | ASSIGNMENTP | ASSIGNMENTS);

methodCall : 
            ( methodName LPAREN ((expr COMMA?)+)? RPAREN
            | CALLOUT LPAREN STRINGLITERAL (COMMA (calloutArg COMMA?)+)? RPAREN);

calloutArg: (expr | STRINGLITERAL);

// This is a "named indentifier" so a method is generated to access it through
// the MethodDeclContext object.
methodName: IDENTIFIER;

location: (IDENTIFIER | IDENTIFIER LBRACE expr RBRACE);

// MINUS causes an ambguity check to be raised because it's not clear
// if "x-foo()"" is "- foo()" or "-foo()". 
// ANTLR has a "longest-match" rule that is used in these situations.
expr:   MINUS expr 
    |   NOT expr
    |   expr (MULTIPLY | DIVISION | MODULO) expr
    |   expr (ADDITION | MINUS) expr
    |   expr (LESSTHAN | GREATERTHAN | LSSTHNEQTO | GRTTHNEQTO) expr
    |   expr (EQUAL | NOTEQUAL) expr
    |   expr AND expr
    |   expr OR expr
    |   location
    |   methodCall
    |   (INTLITERAL | CHARLITERAL | BOOLEANLITERAL | STRINGLITERAL)
    |   LPAREN expr RPAREN
    ;