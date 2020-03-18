class test107{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2 extends Test {

    public boolean next() {

	return true;
    }

}

class Test {

    Test2 test2;
    boolean b;

    public int start(){

        b = test2.next();

	return 0;
    }

    public int next(){ // TE

	return 0;
    }
}
