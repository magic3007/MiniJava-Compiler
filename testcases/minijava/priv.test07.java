class test07{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){

	i = 1;

	while (i < 5) {
		System.out.println(i);
		i = i + 1;
	}

	return i;
    }
}
