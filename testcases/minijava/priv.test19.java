// test19: simple inheritance
//
// correct output: 456

class test19 {
    public static void main(String[] args) {
	System.out.println(new B22().go(new B22()));
    }
}

class A22 {
    boolean b1;
    int i1;

    public int getI1(boolean b) {
	i1 = 123;
	return i1;
    }
}

class B22 extends A22 {
    public int go(A22 a) {
	return a.getI1(true);
    }

    public int getI1(boolean b) {
	if(b) {
	    i1 = 456;
	}
	else {
	    i1 = 789;
	}

	return i1;
    }
}
