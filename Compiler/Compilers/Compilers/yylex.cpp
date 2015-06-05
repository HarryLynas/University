#include <stdio.h>
#include <ctype.h>
#include "yylex.h"

/* Global Variables
*/
int ch = 0;
int yylval = 0;
char yytext[] = { 0 };

/* Return tokens identifying reserved words.
 It would be sensible to use a symbol table instead 
 of hard coding the names.
*/
int reserved(char name[])
{
	if (strcmp(name, "int") == 0) { yylval = POINTER; return INT; }
	if (strcmp(name, "char") == 0) { yylval = POINTER; return CHAR; }
	if (strcmp(name, "float") == 0) { yylval = POINTER; return FLOAT; }
	if (strcmp(name, "void") == 0) return VOID;
	if (strcmp(name, "main") == 0) return MAIN;
	if (strcmp(name, "return") == 0) return RETURN;
	if (strcmp(name, "//") == 0)
	{
		ch = getchar();
		while (ch != NEWLINE && ch != EOF)
			ch = getchar();
		return yylex();
	}
	if (strcmp(name, "/*") == 0)
	{
		while (ch != EOF)
		{
			yylval = ch;
			ch = getchar();
			if (yylval == '*' && ch == '/')
			{
				ch = getchar();
				return yylex();
			}
		}
	}
	return IDENTIFIER;
}

int yylex()
{
	int i;
	switch (ch)
	{
	/* Skip white space. */
	case SPACE:
	case TAB:
	case NULL:
	case NEWLINE:
		ch = getchar();
		return yylex();

	/* Skip comment or recognise division symbol
	"/". 
	*/

	/* Pass EOF.
	WARNING: EOF must not clash with any other
	token. Usually EOF == -1.
	*/
	case EOF:
		return EOF;

	/* Return single character tokens as their
	native code, excepting divide '/' which
	is confusable with a comment symbol and
	is handled above.
	NOTE: The native character set is
	probably ASCII.
	WARNING: Single character tokens must not
	clash with multi-character tokens.
	*/

	case ',' :
	case ';' :
	case '.' :
	case '(' :
	case ')' :
	case '{' :
	case '}' :
	case '<' :
	case '>' :
	case '+' :
	case '-' :
	case '=' :
	case '#' :
	case '*' :
		yylval = ch;
		ch = getchar();
		return yylval;

	/* Return an integer.
	WARNING : Arithmetic overflow will cause
	the lexical analyser to crash.
	*/
	case '0' :
	case '1' :
	case '2' :
	case '3' :
	case '4' :
	case '5' :
	case '6' :
	case '7' :
	case '8' :
	case '9' :
		yylval = ch - '0';
		while (isdigit(ch = getchar()))
			yylval = yylval*10 + ch - '0';
		return INTEGER;

	/* Return an identifier. Unrealistic. */
	case 'A' :
	case 'B' :
	case 'C' :
	case 'D' :
	case 'E' :
	case 'F' :
	case 'G' :
	case 'H' :
	case 'I' :
	case 'J' :
	case 'K' :
	case 'L' :
	case 'M' :
	case 'N' :
	case 'O' :
	case 'P' :
	case 'Q' :
	case 'R' :
	case 'S' :
	case 'T' :
	case 'U' :
	case 'V' :
	case 'W' :
	case 'X' :
	case 'Y' :
	case 'Z' :
	case 'a' :
	case 'b' :
	case 'c' :
	case 'd' :
	case 'e' :
	case 'f' :
	case 'g' :
	case 'h' :
	case 'i' :
	case 'j' :
	case 'k' :
	case 'l' :
	case 'm' :
	case 'n' :
	case 'o' :
	case 'p' :
	case 'q' :
	case 'r' :
	case 's' :
	case 't' :
	case 'u' :
	case 'v' :
	case 'w' :
	case 'x' :
	case 'y' :
	case 'z' :
		for (yytext[0] = ch, i=1; isalnum(ch = getchar()); yytext[i++] = ch);
			yytext[i] = 0;
		return reserved(yytext);
	case '/' :
		yylval = ch;
		ch = getchar();
		if (ch == '/')
			return reserved("//");
		else if (ch == '*')
			return reserved("/*");
		return yylval;

	/* Unexpected character. */
	default :
		yylval = ch;
		ch = getchar();
		return UNKNOWN;
	}
}