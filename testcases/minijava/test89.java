class test89{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    int i;

    public int start(){

	test = test.next(test.third(i));
	
	return 0;
    }

    public Test next(Test t){

	return test;
    }

    public Test third(int i){

	return test;
    }
}
