class test02{
    public static void main(String[] a){
	System.out.println(new Operator().compute());
    }
}

class Operator{
    
    boolean result;

    public int compute(){

	result = 10 < 20 ;

	return 0;
    }
}
