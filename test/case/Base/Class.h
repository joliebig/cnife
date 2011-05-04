#include <iostream>
using namespace std;

class Class {
public:
	Class();
	~Class();
#if A
	int k;
#endif

	void print1();
	void print2();
	void print3();
};

void Class::print1() {
	int k = 10;
	switch(k) {
	case 1:
		print1();
		break;
#if A
	case 2:
		print2();
		break;
#endif
	default:
		print3();
		break;
	}
}
