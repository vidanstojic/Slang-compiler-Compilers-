grammar Slang;

// PARSERSKA GRAMATIKA

start
    :  statement* EOF
    ;

statement
    : simpleStatement
    | ifStatement
    | elseStatement
    | loopStatement
    | printStatement
    | scanStatement
    | functionDefinition
    | functionCallStatement
    | block
    | array
    ;
simpleStatement
    : variableType ID ('=' expr)? ';'
    | ID '=' expr ';'
    ;
variableType
    : NUMBER_KEYWORD
    | BOOLEAN_KEYWORD
    | VOID_KEYWORD
    ;
array
    : ARRAY_KEYWORD variableType ID '[' (NUMBER_LITERAL)?']' '=' '{' expr (','expr)* '}'';'
    ;

ifStatement
    : IF_KEYWORD '(' (('!' expr)? | (expr ('<' | '>' | '<=' | '>=' | '==' | '&&' | '||' ) expr) ) ')' '{' (statement)* '}'
    ;

elseStatement
    : ELSE_KEYWORD '{'(statement)* '}'
    ;

//bool flag = true
//for(int i = 0;!flag;++i)
loopStatement
    : FOR_KEYWORD '(' (NUMBER_KEYWORD ID '=' expr)? ';' ((BANG)? ID (('<' | '>' | '<=' | '>=' | '&&' | '||') expr)? )+ ';' (ID ('+' | '-' | '*' | '/') expr)? ')''{' (statement)* '}'
    | WHILE_KEYWORD '(' ((BANG)? ID (('<' | '>' | '<=' | '>='| '&&' | '||') expr)?)+  ')''{' (statement)* '}'
    ;


functionDefinition
    :  FUNCTION_KEYWORD (variableType | VOID_KEYWORD) ID '(' (functionParameter? (',' functionParameter)*?) ')' '{' (statement)* RETURN_KEYWORD (VOID_KEYWORD | NUMBER_LITERAL | BOOLEAN_LITERAL | ID) ';' '}' // ispraviti da budu tipovi za argumente
    ;

functionParameter
    : variableType ID
    ;

functionCallStatement
    : ID '(' (expr? (',' expr)*?) ')' ';'
    ;

printStatement
    : PRINT_KEYWORD '(' expr(','expr)*')'';'
    ;

scanStatement
    : SCAN_KEYWORD '('expr')'';'
    ;

expr
    : functionCallStatement
    | expr (AND | OR) relationalOperands
    | BANG  expr
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
block: '{' statement* '}';


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