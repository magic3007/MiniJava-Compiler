class test67{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
    int i;

    public int start(){
	
	test = test.next();

	return 0;
    }

    public Test next() {

	return i;	// TE
    }
}
