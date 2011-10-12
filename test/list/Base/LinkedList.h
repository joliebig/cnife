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
#if DLINKED
		Node* last;
#endif

};

LinkedList::LinkedList() {
	first = NULL;
#if DLINKED
	last = NULL;
#endif
	return;
}

LinkedList::~LinkedList() {
	deleteAllNodes();
	return;
}

void LinkedList::insert( int newData ) {
	Node *n = new Node();
	n->data = newData;
#if SORTALGO_EQ_BUBBLESORT || SORTALGO_EQ_INSERTIONSORT
	Node *a = NULL;
	Node *b = NULL;
#endif
#if SORTALGO_EQ_BUBBLESORT
	Node *c = NULL;
	Node *e = NULL;
	Node *tmp = NULL;
#endif

	if (NULL == first) {
		first = n;
	} else {
#if SORTALGO_EQ_INSERTIONSORT
		a = first;
		b = first->next;

		if (first->data
#if SORTORDER_EQ_UPWARD
			>
#else
			<
#endif
			n->data) {
#endif
			n->next = first;
#if DLINKED
			first->prev = n;
#endif
			first = n;
#if SORTALGO_EQ_INSERTIONSORT
			return;
		}
		while (NULL != b && b->data
#if SORTORDER_EQ_UPWARD
			<
#else
			>
#endif
			n->data) {
			a = a->next;
			b = b->next;
		}
		a->next = n;
		n->next = b;
#if DLINKED
		n->prev = a;
		if (NULL != b)
			b->prev = n;
#endif
#endif
	}

#if SORTALGO_EQ_BUBBLESORT
	while (e != first->next) {
		c = a = first;
		b = a->next;
		while (a != e) {
			if (a->data
#if SORTORDER_EQ_UPWARD
				>
#else
				<
#endif
				b->data) {
				if (a == first) {
					tmp = b->next;
					b->next = a;
					a->next = tmp;
#if DLINKED
					a->prev = b;
#endif
					first = b;
					c = b;
				} else {
					tmp = b->next;
					b->next = a;
					a->next = tmp;
					c->next = b;
#if DLINKED
					b->prev = c;
					a->prev = b;
#endif
					c = b;
				}
			} else {
				c = a;
				a = a->next;
			}
			b = a->next;
			if (b == e)
				e == a;
		}
	}
#endif
}

void LinkedList::deleteAllNodes() {
	Node *one;
	Node *two;

	if (NULL == first)
		return;

	while (NULL != two) {
		one = first;
		first = two = first->next;
		one->next = NULL;
#if DLINKED
		if (two != NULL)
			two->prev = NULL;
#endif
		delete one;
	}
}

bool LinkedList::isEmpty() {
	return first == last;
}


void LinkedList::displayAllNodes() {
	Node *pCur = first;
	int nodeCount = 1;

	while ( pCur ) {
		cout << "Node " << nodeCount << ": ";
		displayNode( pCur );
		cout << endl;

		nodeCount++;
		pCur = pCur->next;
	}

	return;
}


void LinkedList::displayNode( Node *node ) {
	cout << node -> data;
	return;
}

void printMenu() {
	cout << "1. Add node " << endl;
	cout << "2. Delete all nodes" << endl;
	cout << "3. Is the list empty?" << endl;
	cout << "4. Display all nodes" << endl;
	cout << "5. Quit" << endl;
}

int getChoice() {
	int choice;

	cout << "Select choice: ";
	cin >> choice;
	cin.clear();
	cin.ignore( 200, '\n' );
	return choice;
}

int getData() {
	int data;

	cout << "Enter data: ";
	cin >> data;
	cin.clear();
	cin.ignore( 200, '\n' );

	return data;
}

void processChoice( int choice, LinkedList& list ) {
	int data;
	bool opStatus;

	switch ( choice ) {
		case 1: data = getData();
				list.insert( data );
				cout << "Node " << data
					<< " added";
				cout << endl;
				break;
		case 2: list.deleteAllNodes();
				cout << "All nodes deleted" << endl;
				break;
		case 3: cout << ( list.isEmpty() ?
						"List is empty" : "List is not empty" );
				cout << endl;
				break;
		case 4: list.displayAllNodes();
				break;
		case 5: break;
		default: cout << "Invalid choice" << endl;
	}

}

int main() {
	LinkedList list;
	int choice;
	do
	{
		printMenu();
		choice = getChoice();
		processChoice( choice, list );

	} while ( choice != 5 );

	return 0;
}
