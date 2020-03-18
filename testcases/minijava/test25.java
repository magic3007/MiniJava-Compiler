class test25{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    boolean result;
	
    public int start(){

	int[] op;

	op = new int[10];

	result = !op;	// TE

	return 0;
    }
}
