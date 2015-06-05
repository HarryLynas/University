#ifndef H_TREE
#define H_TREE

#include <stdint.h>
#include <stdio.h>
#include <string>

struct node
{
	uint32_t value;
	std::string variable;
	node * left;
	node * right;
	bool warning;

	node()
	{
		left = NULL;
		right = NULL;
		variable = "";
		warning = false;
	}

	node(uint32_t val)
	{
		value = val;
		variable = "";
		left = NULL;
		right = NULL;
		warning = false;
	}

	node(uint32_t val, std::string _variable)
	{
		value = val;
		variable = _variable;
		left = NULL;
		right = NULL;
		warning = false;
	}

	node(uint32_t val, std::string _variable, bool warn)
	{
		value = val;
		variable = _variable;
		left = NULL;
		right = NULL;
		warning = warn;
	}
};

class tree
{
	public:
		tree();
		~tree();
		void print();

		node * root;
	private:
		void destroy_tree(node * leaf);
		void printLeaf(node * leaf);

		uint32_t pos;
};

#endif