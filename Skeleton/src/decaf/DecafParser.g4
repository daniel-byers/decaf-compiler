
parser grammar DecafParser;
options { tokenVocab = DecafLexer; }


program: CLASS IDENTIFIER LCURLY field_decl* method_decl* RCURLY EOF;

field_decl: type ((IDENTIFIER | IDENTIFIER LBRACE INTLITERAL RBRACE) COMMA*)+ EOL;

method_decl:
  (type | VOID) IDENTIFIER LPAREN ((type IDENTIFIER COMMA*)+)? RPAREN block;

block: LCURLY var_decl* statement* RCURLY;

var_decl: type (IDENTIFIER COMMA*)+ EOL;

type: (INT | BOOLEAN);

statement : 
          ( location assign_op expr EOL
          | method_call EOL
          | IF LPAREN expr RPAREN block (ELSE block)?
          | FOR IDENTIFIER ASSIGNMENT expr COMMA expr block
          | RETURN expr EOL
          | BREAK EOL
          | CONTINUE EOL
          | block
          );

assign_op: (ASSIGNMENT | ASSIGNMENTP | ASSIGNMENTS);

method_call : 
            ( method_name LPAREN (expr COMMA)? RPAREN
            | CALLOUT LPAREN STRINGLITERAL (COMMA* (callout_arg COMMA)+)? RPAREN);

callout_arg: (expr | STRINGLITERAL);

method_name: IDENTIFIER;

location: (IDENTIFIER | IDENTIFIER LBRACE expr RBRACE);

expr:   MINUS expr 
    |   expr (MULTIPLY | DIVISION | MODULO) expr
    |   expr (ADDITION | MINUS) expr
    |   expr (LESSTHAN | GREATERTHAN | LSSTHNEQTO | GRTTHNEQTO) expr
    |   expr (EQUAL | NOTEQUAL) expr
    |   expr LOGICAND expr
    |   expr LOGICOR expr
    |   location
    |   method_call
    |   (INTLITERAL | CHARLITERAL | BOOLEANLITERAL)
    |   LPAREN expr RPAREN
    ;