class test60{
    public static void main(String[] a){
	System.out.println(new A().start());
    }
}

class C extends B{

}

class B extends A{

}

class A{

    B b;
    C c;

    public int start(){

	c = b;	// TE
	
	return 0;
    }
}
