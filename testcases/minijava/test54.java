class test54{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2{
}

class Test{

    Test test;
    Test2 test2;
    int result;
	
    public int start(){

	test = test2;	// TE

	return 0;
    }
}
