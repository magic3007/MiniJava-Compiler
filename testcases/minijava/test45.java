class test45{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{
	
    public int start(){

	int[] op;

	op = new int[10];

	op[true] = 5;	// TE

	return 0;
    }
}
