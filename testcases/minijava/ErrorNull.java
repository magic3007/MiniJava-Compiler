class ErrorNull{
    public static void main(String[] a){
    	A x; A y;
    	int ret;
    	x = new A();
    	// y = x.haha();
    	// ret = y.foo();
    }
}

class A {
	A n;
	public A haha() {
		return n;
	}
	public int foo() { return 1; }
}

