
#ifndef CODEGENERATION_H
#define CODEGENERATION_H

#include "tree.h"
#include "tokens.h"
#include "yylex.h"
#include <fstream>

void codeGenerateFromTree(tree & AST, std::string fileName,
	std::unordered_map<std::string, SymbolTableRecord*> * SymbolTable);
void parseTreeNode(node * leaf, std::fstream & file,
	std::unordered_map<std::string, SymbolTableRecord*> * SymbolTable);
void peepHoleOptimisation(tree & AST,
	std::unordered_map<std::string, SymbolTableRecord*> * SymbolTable);
void peepTreeNode(node * leaf,
	std::unordered_map<std::string, SymbolTableRecord*> * SymbolTable);

#endif