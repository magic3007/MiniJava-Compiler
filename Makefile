.PHONY: clean javacc cleanjavacc run

OUT = out
SRC = src
JAVA = java
JAVAC = javac
JFLAGS = -g
JTB = deps/jtb132.jar
JAVACC = deps/javacc-7.0.5.jar

default: javacc build

javacc: cleanjavacc
	mkdir $(SRC)/minijava

	$(JAVA) -jar $(JTB) \
		-p minijava \
		-o $(SRC)/minijava/minijava.jbt.jj \
		$(SRC)/minijava.jj
	mv syntaxtree $(SRC)/minijava
	mv visitor $(SRC)/minijava

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

build:
	$(JAVAC) -d $(OUT) \
		--source-path $(SRC) \
		$(SRC)/*.java
	$(JAVA) -cp $(OUT) TypeCheck < testcases/minijava/TreeVisitor.java

run: build
	$(JAVA) -cp $(OUT) HelloWorld

clean: cleanjavacc
	rm -rf out


