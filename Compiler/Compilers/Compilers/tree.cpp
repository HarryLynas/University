#include "tree.h"

tree::tree()
{
	pos = 0;
	root = NULL;
}

tree::~tree()
{
	destroy_tree(root);
}

void tree::destroy_tree(node * leaf)
{
	if (leaf->left != NULL)
		destroy_tree(leaf->left);
	if (leaf->right != NULL)
		destroy_tree(leaf->right);
	delete leaf;
}

void tree::print()
{
	printLeaf(root);
}

void tree::printLeaf(node * leaf)
{
	printf("%d\n", leaf->value);
	if (leaf->right != NULL)
		printf("R -> %d\n", leaf->right->value);
	else
		printf("R -> NULL\n");
	if (leaf->left != NULL)
		printf("L -> %d\n", leaf->left->value);
	else
		printf("L -> NULL\n");
	if (leaf->left != NULL)
		printLeaf(leaf->left);
	if (leaf->right != NULL)
		printLeaf(leaf->right);
}