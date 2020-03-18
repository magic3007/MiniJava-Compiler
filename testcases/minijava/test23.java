class test23{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
	
    public int start(){

	int[] op;

	op = new int[10];

	op[test.next()] = 20;		// TE

	return 0;
    }

    public Test next() {

	return test;
    }
}
