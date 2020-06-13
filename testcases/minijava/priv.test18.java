// test18: simple inheritance
//
// correct output: 123

class test18 {
    public static void main(String[] args) {
	System.out.println(new A21().start());
    }
}

class A21 {
    int i1;

    public int start() {
	return new B21().getI1();
    }

    public int getI1() {
	i1 = 123;
	return i1;
    }
}

class B21 extends A21 {
}
