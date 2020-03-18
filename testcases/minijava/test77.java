class test77{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    int i;

    public int start(){

	i = test.next();
	
	return 0;
    }

    public int next(){

	return test.start();
    }

}
