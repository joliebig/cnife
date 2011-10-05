#include "Config.cpp"

class Node {
public:
	int data;
	Node *next;
#if DLINKED
	Node *prev;
#endif

#if DLINKED2
	void insert( int );
#endif
};


void Node::insert( int newData ) {
    if (a
#if defined(C)
        && b
#endif
        && c) { newData = 5; }
}

