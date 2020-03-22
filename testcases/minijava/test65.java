class test65{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;

    public int start(){
	
	test = test.next();

	return 0;
    }

    public Test next() {

	return true;	// TE
    }
}
