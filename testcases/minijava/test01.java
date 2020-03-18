class test01{
    public static void main(String[] a){
	System.out.println(new Operator().compute());
    }
}

class Operator{
    
    boolean result;

    public int compute(){

	result = true && false;

	return 0;
    }
}
