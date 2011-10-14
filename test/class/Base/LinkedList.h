#include <iostream>
#include "Config.h"
#include "Node.h"

#if DLINKED
class LinkedList {
	public:
		LinkedList();
		~LinkedList();

		void insert( int );
		void deleteAllNodes();

		bool isEmpty();
		void displayAllNodes();

private:
		void displayNode( Node* );

		Node* first;
		Node* last;

};
#endif
