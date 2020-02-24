class Info {
	static final boolean DEBUG = false;

	private static void dump(String... msg) {
		if (! DEBUG) {
			// pass
		} else {
			for (String m : msg) {
				System.out.print(m);
				System.out.print(" ");
			}
			System.out.println();
		}
	}

	static void panic(String... msg) {
		dump(msg);
		System.out.println("Type error");
		throw new RuntimeException("DEBUG");
	}

	static void debug(String... msg) {
		dump(msg);
	}
}
