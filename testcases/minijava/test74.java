class test74{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2 extends Test{

    public int start(){

	Test test1;

	test1 = test;

	return 0;
    }
}

class Test {

    Test test;

    public int start(){

	test = this;
	
	return 0;
    }

}
