/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

parser grammar DecafParser;
options { tokenVocab = DecafLexer; }

// top level. The context of the whole Decaf application.
program: CLASS IDENTIFIER LCURLY fieldDecl* methodDecl* RCURLY EOF;

// global scope. Arrays are only allowed there!
// int a; int a[10]; int a, b; int a[1], b, c;
fieldDecl : type (IDENTIFIER | arrayDecl) (COMMA (IDENTIFIER | arrayDecl))* EOL;

// This is a "named indentifier" so a method is generated to access it through
// the FieldDeclContext object.
arrayDecl: IDENTIFIER LBRACE INTLITERAL RBRACE;

methodDecl:
  (type | VOID) methodName LPAREN ((type IDENTIFIER) (COMMA type IDENTIFIER)*)? RPAREN block;

// This is a "named indentifier" so a method is generated to access it through
// the MethodDeclContext object.
methodName: IDENTIFIER;

// Variables have to be declared first.
block: LCURLY varDecl* statement* RCURLY;

// local scope
varDecl: type IDENTIFIER (COMMA IDENTIFIER)* EOL;

// basic types.
type: (INT | BOOLEAN | VOID);

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
            ( methodName LPAREN (expr (COMMA expr)*)? RPAREN
            | CALLOUT LPAREN STRINGLITERAL (COMMA calloutArg (COMMA calloutArg)*)? RPAREN);

calloutArg: (expr | STRINGLITERAL);

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