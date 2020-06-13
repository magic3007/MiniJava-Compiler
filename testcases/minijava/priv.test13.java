class test13{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int[] i;

    public int start(){

	i = new int[10];

	i[1] = 40;

	i[((3 * 4) - 15) + 4] = 80;

	return i[1];
    }
}
