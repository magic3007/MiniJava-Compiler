class test96{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2 extends Test{

}

class Test {

    Test test;
    int[] i;

    public int start(){

	i = new int[10];

	test = ((new Test2()).next()).next();
	
	return 0;
    }

    public Test next() {
	
	return test;
    }
}
