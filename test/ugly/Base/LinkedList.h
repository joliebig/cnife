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


void LinkedList::insert( int newData ) {
	if (newData > 5) {
		newData = 5;
	}
#if DLINKED3
	else {
		newData = 4;
	}
#endif
}

