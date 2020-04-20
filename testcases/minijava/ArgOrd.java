class argg {
    public static void main(String[] a){
        Test t;
	    t = new Test();
        System.out.println(t.start(t.f0(), t.f1()));
    }
}

class Test {

    public int f0() {
        System.out.println(12);
        return 1;
    }
    public int f1() {
        System.out.println(6);
        return 2;
    }

    public int start(int a, int b) {
        return 3;
    }
}
