grammar Slang;

// PARSERSKA GRAMATIKA

start
    :  statement* EOF
    ;

statement
    : simpleStatement
    | ifStatement
    | loopStatement
    | printStatement
    | scanStatement
    | functionDefinition
    | functionCallStatement
    ;

simpleStatement
    : NUMBER_KEYWORD ID ('=' expr)? ';'
    | BOOLEAN_KEYWORD ID ('=' expr)? ';'
    | ID '=' expr ';'
    | ARRAY_KEYWORD NUMBER_KEYWORD ID ('=' '(' expr(','expr)* ')' )?';'
    ;

ifStatement
    : IF_KEYWORD '(' expr('<' | '>' | '<=' | '>=' | '==') expr ')' '{'(statement)* '}' (elseStatement)?
    ;

elseStatement
    : ELSE_KEYWORD '{'(statement)* '}'
    ;


loopStatement
    : FOR_KEYWORD '(' (NUMBER_KEYWORD ID '=' expr)? ';' ID ('<' | '>' | '<=' | '>=') expr ';' (ID ('+' | '-' | '*' | '/') expr)? ')''{' (statement)* '}'
    | WHILE_KEYWORD '(' ID ('<' | '>' | '<=' | '>=') expr  ')''{' (statement)* '}'
    ;


functionDefinition
    :  FUNCTION_KEYWORD ID '(' (expr? (',' expr)*?) ')' '{' (statement)* RETURN_KEYWORD (expr | VOID_KEYWORD) ';' '}' // ispraviti da budu tipovi za argumente
    ;

functionCallStatement
    : ID '(' (expr? (',' expr)*?) ')' ';'
    ;

printStatement
    : PRINT_KEYWORD '(' expr(','expr)*')'';'
    ;

scanStatement
    : SCAN_KEYWORD '('ID')'';'
    ;

expr
    : functionCallStatement
    | expr (AND | OR) relationalOperands
    | relationalOperands
    ;

relationalOperands
    : relationalOperands(GREATERTHAN | LESSTHAN | LESSTHANOREQ | GREATERTHANOREQ | EQUALTO) addSubOperands
    | addSubOperands
    ;

addSubOperands
    : addSubOperands(ADD | SUB) mulDivOperands
    | mulDivOperands
    ;

mulDivOperands
    : mulDivOperands(MUL | DIV) core
    | core
    ;

core
    : NUMBER_LITERAL
    | BOOLEAN_LITERAL
    | ID
    ;


// LEKSICKA GRAMATIKA

IF_KEYWORD: 'check';
ELSE_KEYWORD: 'backup';
FOR_KEYWORD: 'spin';
WHILE_KEYWORD: 'replay';
NUMBER_KEYWORD: 'numero';
BOOLEAN_KEYWORD: 'yeahNah';
RETURN_KEYWORD: 'getback';
VOID_KEYWORD: 'empty';
ARRAY_KEYWORD: 'squad';
PRINT_KEYWORD: 'dropmsg';
SCAN_KEYWORD: 'grabmsg';
FUNCTION_KEYWORD: 'action';
// LITERALS
BOOLEAN_LITERAL: 'true' | 'false';
NUMBER_LITERAL: ('-')? [0-9]+ ('.' [0-9]+)?;

// Separators

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
LBRACK : '[';
RBRACK : ']';
SEMI   : ';';
COMMA  : ',';
DOT    : '.';

// Operators

ASSIGN   : '=';
GREATERTHAN       : '>';
LESSTHAN       : '<';
BANG     : '!';
COLON    : ':';
EQUALTO    : '==';
LESSTHANOREQ       : '<=';
GREATERTHANOREQ       : '>=';
NOTEQUAL : '!=';
AND      : '&&';
OR       : '||';
ADD      : '+';
SUB      : '-';
MUL      : '*';
DIV      : '/';
BITAND   : '&';
BITOR    : '|';
MOD      : '%';
CARET    : '^';
// IDENTIFIERS
ID : [a-zA-Z] [a-zA-Z0-9]* ; // match usual identifier spec

// COMMENTS AND SPACES
SPACES: [ \u000B\t\r\n\p{White_Space}] -> skip;
COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;