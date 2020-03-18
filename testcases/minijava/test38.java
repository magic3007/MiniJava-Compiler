class test37{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;

    public int start(){

	while (test) System.out.println(1);	// TE

	return 0;
    }
}
