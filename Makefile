.PHONY: clean javacc cleanjavacc run

OUT = out
SRC = src
JAVA = java
JAVAC = javac
JFLAGS = -g
JTB = deps/jtb132.jar
JAVACC = deps/javacc-7.0.5.jar
PGI = deps/pgi.jar
SPP = deps/spp.jar
KGI = deps/kgi.jar

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

run: build
	$(JAVA) -cp $(OUT) HelloWorld

clean: cleanjavacc
	rm -rf out
	rm -rf syntaxtree vistor
	rm -rf $(PARSER_DIR)


TEST_MJ_DIR = testcases/minijava
TEST_MJ     = $(wildcard $(TEST_MJ_DIR)/*.java)

TEST_PGI_DIR = testcases/piglet
TEST_PGI	 = $(wildcard $(TEST_PGI_DIR)/*.pg)

TEST_SPGI_DIR = testcases/spiglet
TEST_SPGI	 = $(wildcard $(TEST_SPGI_DIR)/*.spg)

test: testmj testpg testkg
	@echo Congrats! You have passed all the test.

testmj: $(patsubst $(TEST_MJ_DIR)/%.java, %.testmj, $(TEST_MJ))
testpg: $(patsubst $(TEST_PGI_DIR)/%.pg, %.testpg, $(TEST_PGI))
testkg: $(patsubst $(TEST_SPGI_DIR)/%.spg, %.testkg, $(TEST_SPGI))

%.testmj: $(TEST_MJ_DIR)/%.java
	@rm -rf $(OUT)/minijava/$*.class
	@$(JAVAC) -d $(OUT)/minijava $< 2>/dev/null;\
	EXIT_CODE=$$?;\
	if [ $$EXIT_CODE -eq 0 ] ; \
		then echo "Program type checked successfully" > $(OUT)/std.output; \
		else echo "Type error" > $(OUT)/std.output; fi;
	@$(JAVA) -cp $(OUT) TypeCheck < $< > $(OUT)/my.output
	@diff $(OUT)/std.output $(OUT)/my.output
	@echo [TypeCheck] passed! $<
	@if [ -e $(OUT)/minijava/$*.class ]; then \
		$(JAVA) -cp $(OUT)/minijava $* > $(OUT)/std.output 2>/dev/null; \
		if [ $$? -ne 0 ] ; then echo "ERROR" >> $(OUT)/std.output; fi; \
		$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -jar $(PGI) > $(OUT)/my.output;\
		diff $(OUT)/std.output $(OUT)/my.output; \
		if [ $$? -eq 0 ];  then \
			echo "[   J2P   ] passed!" $<; \
		else \
			echo "[   J2P   ] failed!" $<; \
			$(JAVA) -cp $(OUT) J2P < $< > $(OUT)/dump.pg && \
			false; \
		fi; \
	fi

%.testpg: $(TEST_PGI_DIR)/%.pg
	@$(JAVA) -cp $(OUT) P2S < $< | $(JAVA) -jar $(SPP) > $(OUT)/my.output
	@if [ $$? -ne 0 ]; then \
		echo "[   P2S   ] failed!" $<; \
		$(JAVA) -cp $(OUT) P2S < $< > $(OUT)/dump.spg && \
		false; \
	fi
	@$(JAVA) -jar $(PGI) < $< > $(OUT)/std.output
	@$(JAVA) -cp $(OUT) P2S < $< | $(JAVA) -jar $(PGI) > $(OUT)/my.output
	@diff $(OUT)/std.output $(OUT)/my.output
	@if [ $$? -eq 0 ]; then \
		echo "[   P2S   ] passed!" $<; \
	else \
		echo "[   P2S   ] failed!" $<; \
		$(JAVA) -cp $(OUT) P2S < $< > $(OUT)/dump.spg && \
		false; \
	fi


%.testkg: $(TEST_SPGI_DIR)/%.spg
	@$(JAVA) -jar $(PGI) < $< > $(OUT)/std.output
	@$(JAVA) -cp $(OUT) S2K < $< | $(JAVA) -jar $(KGI) > $(OUT)/my.output
	@diff $(OUT)/std.output $(OUT)/my.output
	@if [ $$? -eq 0 ]; then \
		echo "[   S2K   ] passed!" $<; \
	else \
		echo "[   S2K   ] failed!" $<; \
		$(JAVA) -cp $(OUT) S2K < $< > $(OUT)/dump.kg && \
		false; \
	fi

