.PHONY: clean javacc cleanjavacc run

OUT = out
SRC = src
JAVA = java
JAVAC = javac
JFLAGS = -g
JTB = deps/jtb132.jar
JAVACC = deps/javacc-7.0.5.jar

javacc: cleanjavacc
	mkdir $(SRC)/minijava

	$(JAVA) -jar $(JTB) \
		-p minijava \
		-o $(SRC)/minijava/minijava.jbt.jj \
		$(SRC)/minijava.jj
	mv syntaxtree $(SRC)/minijava
	mv visitor $(SRC)/minijava
	echo 'abc' | sed 's/b/\'\n'/' 

	awk '{ sub(/PARSER_BEGIN\(MiniJavaParser\)/,"&\n\
		package minijava.parser;\n"); print }' \
		$(SRC)/minijava/minijava.jbt.jj \
		> $(SRC)/minijava/minijava.jbt.package.jj
	$(JAVA) -cp $(JAVACC) javacc \
		-OUTPUT_DIRECTORY=$(SRC)/minijava/parser \
		$(SRC)/minijava/minijava.jbt.package.jj

cleanjavacc:
	rm -rf syntaxtree vistor 
	rm -rf src/minijava

$(OUT)/HelloWorld.class: $(SRC)/HelloWorld.java
	$(JAVAC) -d $(OUT) \
		--source-path $(SRC) \
		$(SRC)/HelloWorld.java

run: $(OUT)/HelloWorld.class
	$(JAVA) -cp $(OUT) HelloWorld

clean: cleanjavacc
	rm -rf out


