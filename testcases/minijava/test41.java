class test37{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;

    public int start(){

	if (test) System.out.println(1); else System.out.println(0); // TE

	return 0;
    }
}
