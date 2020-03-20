class RecrusiveExtend{
    public static void main(String[] a){
        System.out.println(new Test().start());
    }
}

class Test extends Test1{
}

class Test1 extends Test2 {
}

class Test2 extends Test3{
}

class Test3 extends Test1{ // TE
}