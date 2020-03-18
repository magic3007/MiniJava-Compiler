class test51{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
    int result;
	
    public int start(){

	result = test;	// TE

	return 0;
    }
}
