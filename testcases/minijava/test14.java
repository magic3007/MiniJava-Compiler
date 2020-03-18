class test14{
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

	op1int = 10;
	op2int = 20;
	resultbool = op1int - op2int;	// TE

	return 0;
    }
}
