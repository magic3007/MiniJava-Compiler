class test36{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;

    public int start(){

	System.out.println(test.next());	// TE

	return 0;
    }

    public Test next(){

	System.out.println(test.start());

	return test;

    }
}
