lexer grammar DecafLexer;
@headers {
  import java.util.*;  
}
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

// Operators
ARITHMATIC  : ('+' | '-' | '/' | '*' | '%');
COMPARISON  : ('==' | '!=' | '<' | '>' | '<=' | '>=');
LOGIC       : ('&&' | '||' | '~');
ASSIGNMENT  : '=';
COMMA       : ',';
LINEEND     : ';';

// Any number in the range zero to nine.
INTLITERAL : (DECLITERAL | HEXLITERAL);

// Any instance of the boolean expressions of truth and non-truth.
BOOLEANLITERAL : ('true' | 'false');

// One character enclosed within single quotes.
CHARLITERAL : '\'' CHAR '\'';

// Any number of characters enclosed within double quotes.
STRINGLITERAL : '"' CHAR* '"';

// This rule simply ignores any space or newline characters.
WS_ : (' ' | '\n' ) -> skip;

// This rule ignores comments ('//' to the end of the line).
SL_COMMENT : '//' (~'\n')* '\n' -> skip;

// A lower or uppercase letter or underscore, followed by none or more
// alphanumeric characters or underscore.
IDENTIFIER : APLHA ALPHANUM*;

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
CHAR : (ESC | ALPHANUM | NONWORD);