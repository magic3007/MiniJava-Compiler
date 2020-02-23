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


TEST_MJ_DIR = testcases/minijava
TEST_MJ     = $(wildcard $(TEST_MJ_DIR)/*.java)
TEST_MJ_OUT = $(patsubst $(TEST_MJ_DIR)/%.java, %.testmj, $(TEST_MJ))

test: $(TEST_MJ_OUT)
	echo done!

%.testmj: $(TEST_MJ_DIR)/%.java
	@sh -c '$(JAVAC) -d $(OUT)/minijava $< &>/dev/null';\
	EXIT_CODE=$$?;\
	if [ $$EXIT_CODE -eq 0 ] ; \
		then echo "Program type checked successfully" > $(OUT)/std.output; \
		else echo "Type error" > $(OUT)/std.output; fi; 
	$(JAVA) -cp $(OUT) TypeCheck < $< > $(OUT)/my.output
	diff $(OUT)/std.output $(OUT)/my.output
	@echo ========= passed! $< ==========


