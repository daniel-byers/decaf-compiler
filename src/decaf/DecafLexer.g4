/**
 * @author Daniel Byers | 13121312
 * 
 * This code builds on examples provided by the following book:
 * Parr, Terence (2012). The Definitive ANTLR 4 Reference. USA: The Pragmatic Bookshelf. 322.
 */

lexer grammar DecafLexer;

// Keywords:
CLASS     : 'class';
BOOLEAN   : 'boolean';
FOR       : 'for';
BREAK     : 'break';
IF        : 'if';
ELSE      : 'else';
CALLOUT   : 'callout';
INT       : 'int';
RETURN    : 'return';
CONTINUE  : 'continue';
VOID      : 'void';

// Special terminals
LCURLY : '{';
RCURLY : '}';
LBRACE : '[';
RBRACE : ']';
LPAREN : '(';
RPAREN : ')';
COMMA  : ',';
EOL    : ';';

// Operators
ADDITION    : '+';
MINUS       : '-';
DIVISION    : '/';
MULTIPLY    : '*';
MODULO      : '%';

ASSIGNMENT  : '=';
ASSIGNMENTP : '+=';
ASSIGNMENTS : '-=';

EQUAL       : '==';
NOTEQUAL    : '!=';
LESSTHAN    : '<';
GREATERTHAN : '>';
LSSTHNEQTO  : '<=';
GRTTHNEQTO  : '>=';

AND         : '&&';
OR          : '||';
NOT         : 'not' | '!';


// Any number in the range zero to nine.
INTLITERAL : (DECLITERAL | HEXLITERAL);

// Any instance of the boolean expressions of truth and non-truth.
BOOLEANLITERAL : ('true' | 'false');

// One character enclosed within single quotes.
CHARLITERAL : '\'' CHAR '\'';

// Any number of characters enclosed within double quotes.
STRINGLITERAL : '"' CHAR* '"';

// This rule simply ignores any space, newline, tab, linefeed or empty quotes.
WS_ : (' ' | '\n' | '\t' | '\f' | '\'\'' ) -> skip;

// This rule ignores comments ('//' to the end of the line).
SL_COMMENT : '//' (~'\n')* ('\n'|EOF) -> skip;

// A lower or uppercase letter or underscore, followed by none or more
// alphanumeric characters or underscore.
IDENTIFIER : APLHA ALPHANUM*;

fragment
LITERAL: (INTLITERAL | CHARLITERAL | BOOLEANLITERAL | STRINGLITERAL);

fragment
BIN_OP: (ARITH_OP | REL_OP | EQ_OP | COND_OP);

fragment
ARITH_OP: (ADDITION | MINUS | MULTIPLY | DIVISION | MODULO);

fragment
REL_OP: (LESSTHAN | GREATERTHAN | LSSTHNEQTO | GRTTHNEQTO);

fragment
EQ_OP: (EQUAL | NOTEQUAL);

fragment
COND_OP: (AND | OR);

// Fragments to hold certain sets of characters.
fragment
ESC :  '\\' ('n' | 't' | '\\' | '"' | '\'');

fragment
ALPHANUM : (APLHA | DIGIT);

fragment
APLHA : [a-zA-Z_];

fragment
DIGIT : [0-9];

fragment
HEXDIGIT: (DIGIT | [a-fA-F]);

fragment
DECLITERAL : DIGIT DIGIT*;

fragment
HEXLITERAL : '0x' HEXDIGIT HEXDIGIT*;

fragment
NONWORD: [\u0020-\u0021\u0023-\u0026\u0028-\u002F\u003A-\u0040\u005B\u005D\u005E\u0060\u007B-\u007E];

fragment
CHAR : (NONWORD | ESC | ALPHANUM);