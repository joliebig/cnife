#include "Config.cpp"

class Node {
public:
	int data;
	Node *next;
#if DLINKED
	Node *prev;
#endif
};

#if DLINKED2
void Node::test() {

}
#endif
