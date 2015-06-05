#include "yylex.h"
#include "parser.h"
#include "codeGeneration.h"

int main(int argc, char **argv)
{
	// Check we have a input name
	if (argc < 2)
	{
		printf("\nNo input file given.\n");
		return -1;
	}

	// The symbol table
	std::unordered_map<std::string, SymbolTableRecord*> SymbolTable;
	// The parser
	parser Parser(&SymbolTable);

	// Since we are not allowed to modify **argv, copy it
	std::string file = argv[1];

	// First call the lexical analyser on the input file which does a basic tokenisation of the file, "argv[1].out"
	yylex_out(file);

	// Populate symbol table
	yylex_symbolTable(file, SymbolTable);

	// Generate parse tree
	try
	{
		Parser.generate(file);
	}
	catch (const std::runtime_error ex)
	{
		printf("\n%s\n", ex.what());
	}

	// Generate code from parse tree
	codeGenerateFromTree(Parser.abstractSyntaxTree, argv[1], &SymbolTable);
	
	// Garbage collect
	for (std::unordered_map<std::string, SymbolTableRecord*>::const_iterator iter
		= SymbolTable.begin();
		iter != SymbolTable.end(); ++iter)
	{
		delete iter->second;
	}
	SymbolTable.clear();

	// Print all of the tree to the output debug file
	Parser.abstractSyntaxTree.print();

	// Make sure buffer is fully flushed
	fflush(stdout);

	return 0;
}
