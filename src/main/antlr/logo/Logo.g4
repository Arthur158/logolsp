grammar Logo;

// ── Parser Rules ─────────────────────────────────────────────────────────────

program
    : line* EOF
    ;

line
    : statement+
    ;

statement
    : procedureDef
    | repeatStmt
    | foreverStmt
    | whileStmt
    | doWhileStmt
    | doUntilStmt
    | untilStmt
    | forStmt
    | ifStmt
    | makeStmt
    | localMakeStmt
    | outputStmt
    | stopStmt
    | procedureCall
    ;

// TO square :size   ... END
procedureDef
    : TO name=IDENT param* statement* END
    ;

param
    : COLON IDENT
    ;

// REPEAT 4 [ fd 100 rt 90 ]
repeatStmt
    : REPEAT expr block
    ;

// FOREVER [ fd 10 rt 1 ]
foreverStmt
    : FOREVER block
    ;

// WHILE condition [ ... ]
whileStmt
    : WHILE block block
    ;

// DO.WHILE [ ... ] condition
doWhileStmt
    : DO_WHILE block block
    ;

// UNTIL condition [ ... ]
untilStmt
    : UNTIL block block
    ;

// DO.UNTIL [ ... ] condition
doUntilStmt
    : DO_UNTIL block block
    ;

// FOR [ i 1 10 ] [ ... ]   or   FOR [ i 1 10 2 ] [ ... ]
forStmt
    : FOR LBRACKET IDENT expr expr expr? RBRACKET block
    ;

// IF condition [ ... ]  or  IF condition [ ... ] [ ... ]  (second block = else)
ifStmt
    : IF expr block block?
    ;

// MAKE "varname expr
makeStmt
    : MAKE QUOTED_WORD expr
    ;

// LOCALMAKE "varname expr  (alias LMAKE)
localMakeStmt
    : (LOCALMAKE | LMAKE) QUOTED_WORD expr
    ;

// OUTPUT expr  (alias OP)
outputStmt
    : (OUTPUT | OP) expr
    ;

// STOP
stopStmt
    : STOP
    ;

// Any identifier followed by its arguments — turtle commands, user procs, etc.
procedureCall
    : name=IDENT expr*
    ;

block
    : LBRACKET statement* RBRACKET
    ;

// ── Expressions ──────────────────────────────────────────────────────────────
// LOGO traditionally has no operator precedence (left-to-right),
// but we give standard precedence here for practical usability.

expr
    : LPAREN expr RPAREN                            # parenExpr
    | MINUS expr                                    # unaryMinus
    | NOT expr                                      # unaryNot
    | expr op=(STAR | SLASH | REMAINDER) expr       # mulExpr
    | expr op=(PLUS | MINUS) expr                   # addExpr
    | expr op=(EQ | NEQ | LT | GT | LE | GE) expr  # cmpExpr
    | expr op=(AND | OR) expr                       # logicExpr
    | expr POWER expr                               # powerExpr
    | NUMBER                                        # numberLiteral
    | QUOTED_WORD                                   # quotedWord
    | LBRACKET expr* RBRACKET                       # listExpr
    | variable                                      # variableExpr
    | LPAREN IDENT expr* RPAREN                     # parenCall
    | IDENT                                         # bareIdent
    ;

variable
    : COLON IDENT
    ;

// ── Lexer Rules ───────────────────────────────────────────────────────────────

// Control flow keywords
TO          : [Tt][Oo] ;
END         : [Ee][Nn][Dd] ;
REPEAT      : [Rr][Ee][Pp][Ee][Aa][Tt] ;
FOREVER     : [Ff][Oo][Rr][Ee][Vv][Ee][Rr] ;
WHILE       : [Ww][Hh][Ii][Ll][Ee] ;
DO_WHILE    : [Dd][Oo] '.' [Ww][Hh][Ii][Ll][Ee] ;
UNTIL       : [Uu][Nn][Tt][Ii][Ll] ;
DO_UNTIL    : [Dd][Oo] '.' [Uu][Nn][Tt][Ii][Ll] ;
FOR         : [Ff][Oo][Rr] ;
IF          : [Ii][Ff] ;
STOP        : [Ss][Tt][Oo][Pp] ;
OUTPUT      : [Oo][Uu][Tt][Pp][Uu][Tt] ;
OP          : [Oo][Pp] ;
MAKE        : [Mm][Aa][Kk][Ee] ;
LOCALMAKE   : [Ll][Oo][Cc][Aa][Ll][Mm][Aa][Kk][Ee] ;
LMAKE       : [Ll][Mm][Aa][Kk][Ee] ;

// Boolean / logical keywords
AND         : [Aa][Nn][Dd] ;
OR          : [Oo][Rr] ;
NOT         : [Nn][Oo][Tt] ;

// Operators
PLUS        : '+' ;
MINUS       : '-' ;
STAR        : '*' ;
SLASH       : '/' ;
REMAINDER   : '%' ;
POWER       : '^' ;
EQ          : '=' ;
NEQ         : '<>' ;
LT          : '<' ;
GT          : '>' ;
LE          : '<=' ;
GE          : '>=' ;

// Delimiters
COLON       : ':' ;
LBRACKET    : '[' ;
RBRACKET    : ']' ;
LPAREN      : '(' ;
RPAREN      : ')' ;

// Numbers — integers and decimals
NUMBER
    : [0-9]+ ('.' [0-9]+)?
    ;

// "word — quoted symbol (used in MAKE, also as a string value)
// The quote character is part of the token so we can distinguish
// "varname (quoted symbol) from :varname (variable reference)
QUOTED_WORD
    : '"' [a-zA-Z_0-9]*
    ;

// Identifiers — must come after all keywords
IDENT
    : [a-zA-Z_] [a-zA-Z_0-9.]*
    ;

// Line comments (;) and block-style comments if needed
COMMENT
    : ';' ~[\r\n]* -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
