class test97{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    int[] i;

    public int start(){

	i = new int[10];

	test = (this.next()).next();
	
	return 0;
    }

    public Test next() {
	
	return test;
    }
}
