/* testlex.c
 Lexical analyser defined in yylex.c
*/
#include <stdio.h>
#include <fcntl.h>
#include "yylex.h"

void yylex_out(std::string & argv)
{
	/*
	• ch contains the next character
	• yylval contains the current character or integer value
	• yytext contains the current string
	*/
	int c;
	extern int yylval;
	extern char yytext[];

	/* Redirect standard input. */
	freopen(argv.c_str(), "r", stdin);

	argv += ".out";
	/* Redirect standard output. */
	freopen(argv.c_str(), "w", stdout);

	/* Run the lexical analyser. */
	while ((c = yylex()) != EOF)
	{
		switch (c)
		{
		case INT : // key word
			printf("KEYWORD int\n");
			break;
		case FLOAT :
			printf("KEYWORD float\n");
			break;
		case CHAR:
			printf("KEYWORD char\n");
			break;
		case VOID :
			printf("KEYWORD void\n");
			break;
		case MAIN :
			printf("KEYWORD main\n");
			break;
		case RETURN :
			printf("KEYWORD return\n");
			break;
		case IDENTIFIER:
			printf("%s VARIABLE\n", yytext);
			break;
		case INTEGER : // actual value
			printf("%d INTEGER\n", yylval);
			break;
		case ',' :
			printf("%d COMMA\n", yylval);
			break;
		case ';' :
			printf("%d SEMICOL\n", yylval);
			break;
		case '.' :
			printf("%cd STOP\n", yylval);
			break;
		case '(' :
			printf("%d LPAR\n", yylval);
			break;
		case ')' :
			printf("%d RPAR\n", yylval);
			break;
		case '{' :
			printf("%d LBRACE\n", yylval);
			break;
		case '}' :
			printf("%d RBRACE\n", yylval);
			break;
		case '<' :
			printf("%d LTHN\n", yylval);
			break;
		case '>' :
			printf("%d RTHN\n", yylval);
			break;
		case '+' :
			printf("%d PLUS\n", yylval);
			break;
		case '-' :
			printf("%d MINUS\n", yylval);
			break;
		case '=' :
			printf("%d ASSIGN\n", yylval);
			break;
		case '#' :
			printf("%d HASH\n", yylval);
			break;
		case '/' :
			printf("%d DIVIDE\n", yylval);
			break;
		case '*' :
			printf("%d TIMES\n", yylval);
			break;
		/* Single character not identified. */
		case UNKNOWN :
			printf("%c %d UNKNOWN\n", yylval, yylval);
			break;
		/* Single character identified as symbol. 
		*/
		default :
			printf("%c NOT_HANDLED\n", c);
		}
	}
}
