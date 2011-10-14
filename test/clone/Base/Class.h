#include <iostream>
using namespace std;

class Class {
public:
	Class();
	~Class();
#if A
	int l;
#endif

	void print1();
	void print2();
};

void Class::print1() {
	int k = 10;
#if A
	k = 5;
#endif
}

void Class::print2() {
	int k = 3;
#if A
	k = 5;
#endif
}
