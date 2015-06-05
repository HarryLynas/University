#ifndef TOKENS_H
#define TOKENS_H

/* Tokens.
*/
/* Punctuation. */
#define COMMA ','
#define SEMICOLON ';'
#define POINT '.'

/* Brackets. */
#define OPENPARENTHESIS '('
#define CLOSEPARENTHESIS ')'
#define OPENLATINBRACE '{'
#define CLOSELATINBRACE '}'
#define OPENANGLE '<'
#define CLOSEANGLE '>'

/* Operators. */
#define PLUS '+'
#define MINUS '-'
#define TIMES '*'
#define DIVIDE '/'
#define ASSIGN '='

/* Key words. */
#define HASH '#'
#define VOID 256
#define INT 257
#define MAIN 258
#define RETURN 261
#define CHAR 262
#define FLOAT 263
#define POINTER 264

/* Variable objects. */
#define IDENTIFIER 259
#define INTEGER 260
#define LOCAL_VARIABLE 499

/* White space. */
#define SPACE ' '
#define TAB '\t'
#define NEWLINE '\n'

/* Catch all.
 Many people use the value zero for unknown, 
 allowing a simple test of whether or not a lexeme 
 is known. But, zero could, theoretically, clash 
 with ASCII null.
*/
#define UNKNOWN -2

#endif