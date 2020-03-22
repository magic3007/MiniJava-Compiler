class test47{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
    int[] op;
	
    public int start(){

	op = new int[10];

	op[test.next()] = 5;	// TE

	return 0;
    }

    public int[] next(){

	return op;
    }
}
