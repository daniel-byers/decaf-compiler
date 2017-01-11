lexer grammar DecafLexer;

// Keywords:
CLASS : 'class';
BOOLEAN : 'boolean';
FOR : 'for';
BREAK : 'break';
IF : 'if';
CALLOUT : 'callout';
INT : 'int';
RETURN : 'return';
CONTINUE : 'continue';
TRUE : 'true';
ELSE : 'else';
VOID : 'void';

// Special terminals
LCURLY : '{';
RCURLY : '}';
LBRACE : '[';
RBRACE : ']';
LPAREN : '(';
RPAREN : ')';

// Operators
UMINUS : '-';
BANG : '!';
MULTIPLY : '*';
DIVIDE : '/';
MODULO : '%';
PLUS : '+';
SUBTRACT : '-';
LESSTHAN : '<';
LSSTNEQTO : '<=';
GREATERTHAN : '>';
GRTRTNEQTO : '>=';
EQUAL : '==';
NOTEQUAL : '!=';
AND : '&&';
OR : '||';
NOT : '~';
ASSIGN : '=';

ID : ('a'..'z' | 'A'..'Z')('a'..'z' | 'A'..'Z' | '0'..'9')+;

NUMBER : ('0'..'9')+;

// This rule simply ignores (skips) any space or newline characters
WS_ : (' ' | '\n' ) -> skip;

// And this rule ignores comments (everything from a '//' to the end of the line)
SL_COMMENT : '//' (~'\n')* '\n' -> skip;

// These two rules incompletely describe characters and strings, and make use of the ESC fragment described below
// This rule says a character is contained within single quotes, and is a single instance of either an ESC, or any
// character other than a single quote.
CHAR : '\'' (ESC|~'\'') '\'';
// This rule says a string is contained within double quotes, and is zero or more instances of either an ESC, or any
// character other than a double quote.
STRING : '"' (ESC|~'"')* '"';

// A rule that is marked as a fragment will NOT have a token created for it. So anything matching the rules above
// will create a token in the output, but something matching the ESC rule below will only be used locally in the scope
// of this file. Any rule that should not generate an output token should be preceded by the fragment keyword.
// ESC matches either a pair of characters representing a newline ('\' and 'n') or a pair of characters representing
// a double quote ('\' and '"'). HINT: there are many other characters that should be escaped - think of how you need
// to write them in strings in languages like Java.
fragment
ESC :  '\\' ('n'|'"');
