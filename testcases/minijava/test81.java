class test81{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    int i;

    public int start(){

	int j;
	int k;
	
	i = 10;
	j = 10;

	k = ((i+((j*5)-8))+23); 

	return k;
    }
}
