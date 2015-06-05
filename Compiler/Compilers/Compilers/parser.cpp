#include "parser.h"

parser::parser(std::unordered_map<std::string, SymbolTableRecord*> * SymbolTable)
{
	this->SymbolTable = SymbolTable;
	lastOperationWasDivide = false;
}

void parser::generate(std::string & fileName)
{
	std::fstream file(fileName);

	if (!findMain(file))
	{
		printf("\nERROR: No main function found at start of program.");
		throw std::runtime_error("No main function found at start of program.");
	}
	
	if (!findFunction(file))
	{
		printf("\nERROR: Semantic errors were found in main, main function is invalid.");
		throw std::runtime_error("Semantic errors were found in main, main function is invalid.");
	}

	while (findCode(file))
	{
		// Nothing to do here
	}

	// Check }
	std::string word;
	file >> word;
	if (word.compare("RBRACE") == 0)
	{
		node * rBRACE = new node(CLOSELATINBRACE);
		abstractSyntaxTree.root->right = rBRACE;
	}

	file.close();
}

bool parser::findCode(std::fstream & file)
{
	std::string word;
	file >> word;
	// Keep reading until } or EOF
	if (file.eof())
	{
		printf("ERROR: Unexpected end of file. Wanted: }");
		throw std::runtime_error("Unexpected end of file. Wanted: }");
	}
	// Check }
	if (word.compare("RBRACE") == 0)
	{
		node * rBRACE = new node(CLOSELATINBRACE);
		abstractSyntaxTree.root->right = rBRACE;
		return false;
	}
	// Check to see if word exists in symbol table (might be a = ...)
	std::unordered_map<std::string, SymbolTableRecord*>::const_iterator got = SymbolTable->find(word);
	if (got != SymbolTable->end())
	{
		node * newRoot = new node(got->second->token, word);
		current->left = newRoot;
		current = newRoot;
		// skip over VARIABLE
		file >> word;
		while (!getEndOfLine(file))
		{
			// check operator (= + -)
			// check variable or value
			if (!getOperator(file) || !getToken(file))
				return false;
		}
		return true;
	}
	// Validation over, now to handle the code that should be here
	// check KEYWORD (int)
	// check variable name (GET TOKEN)
	if (!getKeyword(file, word) || !getToken(file))
		return false;
	// Keep reading operations while ; is not found
	while (!getEndOfLine(file))
	{
		// check operator (= + -)
		// check variable or value
		if (!getOperator(file) || !getToken(file))
			return false;
	}
	return true;
}

bool parser::getKeyword(std::fstream & file, std::string word)
{
	if (word.compare("KEYWORD") == 0)
	{
		file >> word;
		if (word.compare("int") == 0 || word.compare("string") == 0
			|| word.compare("std::string") == 0 || word.compare("float") == 0
			|| word.compare("char") == 0)
		{
			// Insert root saying int data type
			// Should really be the true data type, but everything is converted to 'local' anyway
			// So no need to specify a specific type
			node * newRoot = new node(INT);
			current->left = newRoot;
			
			current = newRoot;

			return true;
		}
		else if (word.compare("return") == 0) {
			// If return we need to make sure the code generation will know
			node * newRoot = new node(RETURN);
			current->left = newRoot;

			current = newRoot;

			return true;
		}
	}
	return false;
}

bool parser::getToken(std::fstream & file)
{
	node * newRoot;
	std::string word;
	// Get var and check it exists in symbol table
	file >> word;
	
	// See if it is a variable
	// First character must be a letter or underscroll
	if (isalpha(word.c_str()[0]) || word.c_str()[0] == '_')
	{
		std::unordered_map<std::string, SymbolTableRecord*>::const_iterator got = SymbolTable->find(word);
		if (got == SymbolTable->end())
			throw std::runtime_error("ERROR: Variable '" + word + "' not found in symbol table.");
		// check for / 0
		if (lastOperationWasDivide && got->second->value.compare("0") == 0)
			// Lua will handle divide by zero fine, just flag a warning
			newRoot = new node(got->second->token, word, true);
		else
			// Generate node with vars token
			newRoot = new node(got->second->token, word);
	}
	else
	{
		// check for / 0
		if (lastOperationWasDivide && word.compare("0") == 0)
			// Lua will handle divide by zero fine so just flag it has been found
			newRoot = new node(LOCAL_VARIABLE, word, true);
		else
			// Generate node with fixed number
			newRoot = new node(LOCAL_VARIABLE, word);
	}

	// Skip over INTEGER || VARIABLE (etc)
	file >> word;
	
	// Add var to tree
	current->left = newRoot;

	return true;
}

bool parser::getOperator(std::fstream & file)
{
	std::string word;
	file >> word;

	// word will be a token
	int op = atoi(word.c_str());

	lastOperationWasDivide = false;

	node * leaf;
	switch (op)
	{
	case PLUS:
		leaf = new node(PLUS);
		break;
	case ASSIGN:
		leaf = new node(ASSIGN);
		break;
	case MINUS:
		leaf = new node(MINUS);
		break;
	case DIVIDE:
		leaf = new node(DIVIDE);
		lastOperationWasDivide = true;
		break;
	case TIMES:
		leaf = new node(TIMES);
		break;
	default:
		throw std::runtime_error("ERROR: Operator '" + word + "' is not supported (displayed in int form).");
	}
	// Skip over what it is
	file >> word;

	// Add leaf to tree and update current
	current->right = leaf;
	current = leaf;
	return true;
}

bool parser::getEndOfLine(std::fstream & file)
{
	// Store current file position
	std::streamoff pos = file.tellg();

	std::string word;
	file >> word;
	file >> word;
	// check if semicolon
	if (word.compare("SEMICOL") == 0)
	{
		node * leaf = new node(SEMICOLON);
		current->right = leaf;
		// current is now ;
		current = leaf;
		return true;
	}

	// Restore current position
	file.seekg(pos);
	return false;
}

bool parser::findFunction(std::fstream & file)
{
	std::string word;
	// Read twice, first is token
	file >> word;
	file >> word;
	if (word.compare("LPAR") == 0)
	{
		node * lBRACK = new node(OPENPARENTHESIS);
		current->left = lBRACK;
		// Read twice, first is token
		file >> word;
		file >> word;
		if (word.compare("RPAR") == 0)
		{
			node * rBRACK = new node (CLOSEPARENTHESIS);
			lBRACK->left = rBRACK;
			// find {
			file >> word;
			file >> word;
			if (word.compare("LBRACE") == 0)
			{
				node * lBRACE = new node(OPENLATINBRACE);
				current->right = lBRACE;
				current = lBRACE;
				return true;
			}
		}
	}
	return false;
}

bool parser::findMain(std::fstream & file)
{
	std::string word;
	file >> word;
	if (word.compare("KEYWORD") == 0)
	{
		file >> word;
		if (word.compare("int") == 0)
		{
			// Insert root saying return of int
			node * newRoot = new node(INT);
			abstractSyntaxTree.root = newRoot;

			file >> word;
			if (word.compare("KEYWORD") == 0)
			{
				file >> word;
				if (word.compare("main") == 0)
				{
					node * main = new node(MAIN);
					newRoot->left = main;
					current = main;
					return true;
				}
			}
		}
	}
	return false;
}