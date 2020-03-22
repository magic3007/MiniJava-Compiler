class test79{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    int i;

    public int start(){

	int j;

	i = 10;
	j = 10;

	return ((i+((j*5)-8))+23); 
    }
}
