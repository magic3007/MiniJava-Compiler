class test63{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test2{

}

class Test{

    Test2 test2;
	
    public int start(){

	test2 = this;	// TE

	return 0;
    }
}
