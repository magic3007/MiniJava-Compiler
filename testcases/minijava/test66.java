class test66{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2 extends Test{

}

class Test{

    Test test;

    public int start(){
	
	test = test.next();

	return 0;
    }

    public Test next() {

	Test2 test2;

	return test2;
    }
}
