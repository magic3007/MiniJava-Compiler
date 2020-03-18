class test20{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int result;
	
    public int start(){

	int[] op;

	op = new int[10];

	result = op.start();		// TE

	return 0;
    }
}
