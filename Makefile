.PHONY: clean javacc cleanjavacc run

OUT = out
SRC = src
JAVA = java
JAVAC = javac
JFLAGS = -g
JTB = deps/jtb132.jar
JAVACC = deps/javacc-7.0.5.jar

default: build

PARSERS = $(wildcard $(SRC)/*.jj)
PARSER_DIR = $(patsubst $(SRC)/%.jj, $(SRC)/%, $(PARSERS))

parser: $(PARSER_DIR)

$(SRC)/%: $(SRC)/%.jj
	mkdir $(SRC)/$*

	$(JAVA) -jar $(JTB) \
		-printer \
		-p $* \
		-o $(SRC)/$*/$*.jbt.jj \
		$(SRC)/$*.jj
	mv syntaxtree $(SRC)/$*
	mv visitor $(SRC)/$*

	awk '{ sub(/PARSER_BEGIN\([a-zA-Z]+\)/,"&\n\
		package $*.parser;\n"); print }' \
		$(SRC)/$*/$*.jbt.jj \
		> $(SRC)/$*/$*.jbt.package.jj
	$(JAVA) -cp $(JAVACC) javacc \
		-OUTPUT_DIRECTORY=$(SRC)/$*/parser \
		$(SRC)/$*/$*.jbt.package.jj

build: parser
	$(JAVAC) -d $(OUT) \
		--source-path $(SRC) \
		$(SRC)/*.java
	$(JAVA) -cp $(OUT) TypeCheck < testcases/minijava/TreeVisitor.java

run: build
	$(JAVA) -cp $(OUT) HelloWorld

clean: cleanjavacc
	rm -rf out
	rm -rf syntaxtree vistor 
	rm -rf $(PARSER_DIR)


TEST_MJ_DIR = testcases/minijava
TEST_MJ     = $(wildcard $(TEST_MJ_DIR)/*.java)

test: $(patsubst $(TEST_MJ_DIR)/%.java, %.testmj, $(TEST_MJ))
	@echo Congrats! You have passed all the test.

%.testmj: $(TEST_MJ_DIR)/%.java
	@sh -c '$(JAVAC) -d $(OUT)/minijava $< &>/dev/null';\
	EXIT_CODE=$$?;\
	if [ $$EXIT_CODE -eq 0 ] ; \
		then echo "Program type checked successfully" > $(OUT)/std.output; \
		else echo "Type error" > $(OUT)/std.output; fi; 
	@$(JAVA) -cp $(OUT) TypeCheck < $< > $(OUT)/my.output
	@diff $(OUT)/std.output $(OUT)/my.output
	@echo ========= passed! $<


