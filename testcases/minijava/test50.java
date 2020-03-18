class test50{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
    int[] op;
	
    public int start(){

	op = new int[10];

	op[5] = test.next();	// TE

	return 0;
    }

    public int[] next(){

	return op;
    }
}
