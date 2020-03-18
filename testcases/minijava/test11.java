class test11{
    public static void main(String[] a){
	System.out.println(new Operator().compute());
    }
}

class Operator{
    
    boolean op1bool;
    boolean op2bool;
    int op1int;
    int op2int;
    int resultint;
    boolean resultbool;

    public int compute(){

	op1bool = true;
	op2bool = false;
	resultint = op1bool && op2bool;	// TE

	return 0;
    }
}
