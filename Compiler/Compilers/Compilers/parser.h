#ifndef H_PARSER
#define H_PARSER

#include <fstream>
#include <stack>
#include <stdexcept>
#include "yylex.h"
#include "tree.h"

class parser
{
	public:
		parser(std::unordered_map<std::string, SymbolTableRecord*> * SymbolTable);

		tree abstractSyntaxTree;

		void generate(std::string & fileName);

	private:
		// Store a reference to the symbol table
		std::unordered_map<std::string, SymbolTableRecord*> * SymbolTable;
		// The current location in the tree
		node * current;
		// Used to convert char -> int -> string
		// each step is required as we want to get the string of the int of the char
		static const int closeRBrace = CLOSELATINBRACE;
		static const int semiColon = SEMICOLON;
		// True if last operation was divide
		bool lastOperationWasDivide;

		// Function prototypes
		bool findMain(std::fstream & file);
		bool findFunction(std::fstream & file);
		bool findCode(std::fstream & file);
		bool getKeyword(std::fstream & file, std::string existingWord);
		bool getToken(std::fstream & file);
		bool getOperator(std::fstream & file);
		bool parser::getEndOfLine(std::fstream & file);
};

#endif