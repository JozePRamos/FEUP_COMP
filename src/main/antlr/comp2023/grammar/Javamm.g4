grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_$][a-zA-Z_0-9$]* ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : statement+ EOF
    |(importDeclaration)* classDeclaration EOF
    ;

importDeclaration
     : 'import' value=ID ( '.' name+=ID )* ';'
     ;

classDeclaration
     : 'class' name=ID ( 'extends' extend=ID )? '{' ( varDeclaration )* ( methodDeclaration )* '}'
     ;

varDeclaration locals[boolean isStatic=false]
    : ('static'{$isStatic=true;})? type variable=ID ';'
    ;


identifier : value=ID ;

 paremeter
   : type nameParameter=ID;

 paremeterlist
   : ( paremeter ( ',' paremeter )* )?;

methodDeclaration
     : ('public')? (invoke=ID)? type name=ID '(' paremeterlist ')' '{' ( varDeclaration
     )* ( statement )* 'return' expression ';' '}'
     | ('public')? 'static' 'void' 'main' '(' type nameParameter=ID ')' '{' ( varDeclaration
     )* ( statement )* '}'
     ;

type locals[boolean isArray=false]
     : (value='int'
     | value='boolean'
     | value='String'
     | value=ID) ('[' ']' {$isArray=true;})?
     ;

//por nomes
statement
    : '{' ( statement )* '}' #Brackets
    | 'if' '(' expression ')' statement 'else' statement #IfStat
    | 'while' '(' expression ')' statement #WhileStat
    | expression ';' #End
    | identifier '=' expression ';' #VarDecl
    | identifier '[' expression ']' '=' expression ';' #Array
    ;

// ver prioridade
expression
    : expression op=('&&' | '||') expression #BinaryOp
    | expression op=('>' | '<') expression #BinaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression '[' expression ']' #ArrayAcess
    | expression '.' 'length' #LenCall
    | expression '.' identifier '(' ( expression ( ',' expression )* )? ')' #Call
    | 'new' 'int' '[' expression ']' #NewArrayInstance
    | 'new' identifier '(' ')'  #NewInstance
    | op='!' expression #Operation
    | op='(' expression ')' #Parentesis
    | value='true' #BooleanValue
    | value='false' #BooleanValue
    | value='this' #ThisAcess
    | value=INTEGER #Integer
    | value=ID #ID
    ;




