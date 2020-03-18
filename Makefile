.PHONY: clean javacc cleanjavacc run

OUT = out
SRC = src
TEMP_DIR = temp
JAVA = java
JAVAC = javac
JFLAGS = -g
JTB = deps/jtb132.jar
JAVACC = deps/javacc-7.0.5.jar
MNJ = deps/minijava-parser.jar
PGI = deps/pgi.jar
SPP = deps/spp.jar

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

TEST_TC_DIR = testcases/minijava
TEST_TC     = $(wildcard $(TEST_TC_DIR)/*.java)

TEST_MJ_DIR = testcases/minijava
TEST_MJ     = $(wildcard $(TEST_MJ_DIR)/*.java)

TEST_PGI_DIR = testcases/piglet
TEST_PGI	 = $(wildcard $(TEST_PGI_DIR)/*.pg)

test: testtc testmj testpg
	@echo Congrats! You have passed all the test.

testtc: $(patsubst $(TEST_TC_DIR)/%.java, %.testtc, $(TEST_TC))
testmj: $(patsubst $(TEST_MJ_DIR)/%.java, %.testmj, $(TEST_MJ))
testpg: $(patsubst $(TEST_PGI_DIR)/%.pg, %.testpg, $(TEST_PGI))

%.testtc: $(TEST_TC_DIR)/%.java
	@$(JAVAC) -cp $(MNJ) $< 2>/dev/null -d $(OUT)/minijava; \
	if [ $$? -eq 0 ] ; \
		then echo "Program type checked successfully" >$(TEMP_DIR)/std.$@.output; \
		else  echo "Type error" > $(TEMP_DIR)/std.$@.output; fi;
# manually add comment 'TE' to identify the type errors that can not be recognized by standard Java compiler
	@grep "//\s*TE" $< >/dev/null; \
	if [ $$? -eq 0 ] ; then echo "Type error" > $(TEMP_DIR)/std.$@.output; fi
# manually add comment 'legal in MiniJava' to identify the type errors that are judged illegal by standard Java compiler but legal in MiniJava
	@grep 'legal in MiniJava' $< >/dev/null; \
	if [ $$? -eq 0 ] ; then echo "Program type checked successfully" > $(TEMP_DIR)/std.$@.output; fi
	@$(JAVA) -cp $(OUT) TypeCheck < $< > $(TEMP_DIR)/my.$@.output
	@diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
	if [ $$? -eq 0 ];  then \
		echo "[TypeCheck] passed!" $<; \
	else \
		echo "[TypeCheck] failed!" $< && \
		false; \
	fi
	# @rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output

%.testmj: $(TEST_MJ_DIR)/%.java
	@$(JAVA) -cp $(MNJ) $< > $(TEMP_DIR)/std.$@.output 2>/dev/null; \
	if [ $$? -ne 0 ] ; then echo "ERROR" >> $(TEMP_DIR)/std.$@.output; fi;
	@$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -jar $(PGI) >$(TEMP_DIR)/my.$@.output
	@diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
	if [ $$? -eq 0 ];  then \
		echo "[   J2P   ] passed!" $<; \
	else \
		echo "[   J2P   ] failed!" $<; \
		$(JAVA) -cp $(OUT) J2P < $< > $(TEMP_DIR)/dump.pg && \
		false; \
	fi
	@rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output

%.testpg: $(TEST_PGI_DIR)/%.pg
	@$(JAVA) -cp $(OUT) P2S < $< | $(JAVA) -jar $(SPP) >$(TEMP_DIR)/my.$@.output; \
	if [ $$? -ne 0 ]; then \
		echo "[   P2S   ] failed!" $<; \
		$(JAVA) -cp $(OUT) P2S < $< > $(TEMP_DIR)/dump.spg && \
		false; \
	fi
	@$(JAVA) -jar $(PGI) < $< > $(TEMP_DIR)/std.$@.output
	@$(JAVA) -cp $(OUT) P2S < $< | $(JAVA) -jar $(PGI) > $(TEMP_DIR)/my.$@.output
	@diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
	if [ $$? -eq 0 ]; then \
		echo "[   P2S   ] passed!" $<; \
	else \
		echo "[   P2S   ] failed!" $<; \
		$(JAVA) -cp $(OUT) P2S < $< > $(OUT)/dump.spg && \
		false; \
	fi
	@rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output
