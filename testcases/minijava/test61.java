class test61{
    public static void main(String[] a){
	System.out.println(new A().start());
    }
}

class C extends B{

}

class B extends A{

}

class A{

    A a;
    C c;

    public int start(){

	c = a;	// TE
	
	return 0;
    }
}
