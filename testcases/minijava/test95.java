class test95{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    int[] i;

    public int start(){

	i = new int[10];

	test = ((new Test()).next()).next();
	
	return 0;
    }

    public Test next() {
	
	return test;
    }
}
