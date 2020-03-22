class test28{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    boolean result;
	
    public int start(){

	result = !this;	// TE

	return 0;
    }
}
