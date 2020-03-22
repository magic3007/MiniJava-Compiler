class test49{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
	
    public int start(){

	int[] op;

	op = new int[10];

	op[5] = test;	// TE

	return 0;
    }
}
