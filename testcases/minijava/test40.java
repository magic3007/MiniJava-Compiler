class test40{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    public int start(){

	if (1) System.out.println(1); else System.out.println(0); // TE

	return 0;
    }
}
