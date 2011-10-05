public class Class1 {
public:
    void foo();
    int a;
};

void Class1::foo() {
    int a;
#if A
    if (a == 1) a = 2;
    else if (a == 2) a = 3;
    else if (a == 3) a = 5;
    else if (a == 4) a = 7;
    else a = 1;
#endif
    int b;
}
