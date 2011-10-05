public class Class {
public:
    void foo();
};

void Class::foo() {
    int a;
#if A
    switch(a) {
    case 1: a = 2; break;
    case 2: a = 3; break;
    case 3: a = 5; break;
    case 4: a = 7; break;
    default: a = 1; break;
    }
#endif
    int b;
}
