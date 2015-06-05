/* yytext.h
 Definitions for yylex.c
*/

#ifndef YYLEX_H
#define YYLEX_H

#include "tokens.h"
#include <string>
#include <unordered_map>
#include <stdint.h>

// Symbol Table Record structure
struct SymbolTableRecord
{
	uint32_t token;			// unqiue variable token name
	std::string lexeme;		// variable name
	std::string value_type;	// int, double, etc
	std::string value;		// the value if it has one
};

// Function prototypes
void yylex_out(std::string & file);
void yylex_symbolTable(std::string & file, std::unordered_map<std::string, SymbolTableRecord*> & SymbolTable);
int reserved(char name[]);
int yylex();

#endif