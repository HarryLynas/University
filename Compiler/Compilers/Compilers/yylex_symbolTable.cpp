#include "yylex.h"
#include <sstream>
#include <fstream>

void yylex_symbolTable(std::string & argv, std::unordered_map<std::string, SymbolTableRecord*> & SymbolTable)
{
	// finish output from before
	fflush(stdout);

	std::fstream file(argv);
	std::string word;
	
	SymbolTableRecord * currentRecord;
	std::string lexeme;

	uint32_t current_token = 500;

	do
	{
		file >> word;
		
		if (strcmp(word.c_str(), "KEYWORD") == 0) // Check lines match
		{
			currentRecord = new SymbolTableRecord();
			// Get the type and save it
			file >> word;
			currentRecord->value_type = word;
			// Get the lexeme
			file >> word;
			lexeme = word;
			currentRecord->lexeme = word;
			currentRecord->value = word;
			file >> word;
			currentRecord->value_type = word;
		}
		else
			continue;

		// Set token and increment
		currentRecord->token = current_token++;

		// Handle duplicate variables/functions
		std::unordered_map<std::string, SymbolTableRecord*>::const_iterator got = SymbolTable.find(lexeme);
		if (got != SymbolTable.end())
			lexeme = "_" + lexeme;
		if (currentRecord->lexeme.compare("KEYWORD") == 0)
			delete currentRecord;
		else
			SymbolTable.insert(std::make_pair(lexeme, currentRecord));
	}
	while (!file.eof());

	file.close();
}
