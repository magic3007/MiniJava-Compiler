class test27{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
    boolean result;
	
    public int start(){

	result = !!true;	

	return 0;
    }

    public Test next() {

	return test;
    }
}
