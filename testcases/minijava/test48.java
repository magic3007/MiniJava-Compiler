class test48{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{
	
    public int start(){

	int[] op;

	op = new int[10];

	op[5] = true;	// TE

	return 0;
    }
}
