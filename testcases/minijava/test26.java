class test26{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
    boolean result;
	
    public int start(){

	result = !test.next();	// TE

	return 0;
    }

    public Test next() {

	return test;
    }
}
