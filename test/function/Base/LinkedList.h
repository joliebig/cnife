#include <iostream>
#include "Config.h"
#include "Node.h"

using namespace std;

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


#ifdef DLINKED
void LinkedList::deleteAllNodes() {
	Node *one;
	Node *two;

	if (NULL == first)
		return;

	while (NULL != two) {
		one = first;
		first = two = first->next;
		one->next = NULL;
		if (two != NULL)
			two->prev = NULL;
		delete one;
	}
}
#endif
