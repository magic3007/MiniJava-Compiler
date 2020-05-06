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
KGI = deps/kgi.jar
SPIM = spim

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
	rm -rf $(OUT) $(TEMP)
	rm -rf syntaxtree vistor
	rm -rf $(PARSER_DIR)

TEST_TC_DIR = testcases/minijava
TEST_TC     = $(wildcard $(TEST_TC_DIR)/*.java)

TEST_MJ_DIR = testcases/minijava
TEST_MJ     = $(wildcard $(TEST_MJ_DIR)/*.java)

TEST_PGI_DIR = testcases/piglet
TEST_PGI	 = $(wildcard $(TEST_PGI_DIR)/*.pg)

TEST_SPGI_DIR = testcases/spiglet
TEST_SPGI	  = $(wildcard $(TEST_SPGI_DIR)/*.spg)

TEST_KGI_DIR = testcases/kanga
TEST_KGI	 = $(wildcard $(TEST_KGI_DIR)/*.kg)
	
test: testtc testmj testpg testkg testmj2spgi testmp # testall
	@echo Congrats! You have passed all the test.

testtc: $(patsubst $(TEST_TC_DIR)/%.java, %.testtc, $(TEST_TC))
testmj: $(patsubst $(TEST_MJ_DIR)/%.java, %.testmj, $(TEST_MJ))
testpg: $(patsubst $(TEST_PGI_DIR)/%.pg, %.testpg, $(TEST_PGI))
testmj2spgi: $(patsubst $(TEST_MJ_DIR)/%.java, %.testmj2spgi, $(TEST_MJ))
testkg: $(patsubst $(TEST_SPGI_DIR)/%.spg, %.testkg, $(TEST_SPGI))
testmp: $(patsubst $(TEST_KGI_DIR)/%.kg, %.testmp, $(TEST_KGI))
testall: $(patsubst $(TEST_MJ_DIR)/%.java, %.testall, $(TEST_MJ))

OkText = "Program type checked successfully"
ErrText = "Type error"

define typecheck
	@if [ ! -d $(TEMP_DIR) ] ; then mkdir -p $(TEMP_DIR); fi
	@$(JAVAC) $<  \
		-d $(OUT) \
		2>/dev/null; \
	if [ $$? -eq 0 ] ; \
		then echo $(OkText) > $(TEMP_DIR)/std.$@.output; \
		else  echo $(ErrText) > $(TEMP_DIR)/std.$@.output; fi;
# manually add comment 'TE' to identify the type errors that can not be recognized by standard Java compiler
	@grep "//\s*TE" $< >/dev/null; \
	if [ $$? -eq 0 ] ; then echo $(ErrText) > $(TEMP_DIR)/std.$@.output; fi
# manually add comment 'legal in MiniJava' to identify the type errors that are judged illegal by standard Java compiler but legal in MiniJava
	@grep 'legal in MiniJava' $< >/dev/null; \
	if [ $$? -eq 0 ] ; then echo  $(OkText) > $(TEMP_DIR)/std.$@.output; fi
endef

%.testall: $(TEST_MJ_DIR)/%.java
	@$(call typecheck)
	@grep $(OkText) $(TEMP_DIR)/std.$@.output >/dev/null; \
	if [ $$? -eq 0 ]; then \
		$(JAVA) $< > $(TEMP_DIR)/std.$@.output 2>/dev/null; \
		if [ $$? -ne 0 ] ; then \
			echo "ERROR" >> $(TEMP_DIR)/std.$@.output; \
			echo "[   ALL   ] Runtime Error(Ignore)." $<; \
		else \
			$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -cp $(OUT) P2S | $(JAVA) -cp $(OUT) S2K | $(JAVA) -jar $(KGI) >$(TEMP_DIR)/my.$@.output; \
			diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
			if [ $$? -eq 0 ];  then \
				echo "[   ALL   ] passed!" $<; \
				rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
			else \
				echo "[   ALL   ] failed!" $<; \
				$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -cp $(OUT) P2S | $(JAVA) -cp $(OUT) S2K > $(TEMP_DIR)/dump.$@.pg && \
				false; \
			fi; \
		fi;\
	else \
		echo "[   ALL   ] Type Error(Ignore)." $<; \
	fi

%.testmj2spgi: $(TEST_MJ_DIR)/%.java
	@if [ ! -d $(TEMP_DIR) ] ; then mkdir -p $(TEMP_DIR); fi
	@$(call typecheck)
	@grep $(OkText) $(TEMP_DIR)/std.$@.output >/dev/null; \
	if [ $$? -eq 0 ]; then \
		$(JAVA) $< > $(TEMP_DIR)/std.$@.output 2>/dev/null; \
		if [ $$? -ne 0 ] ; then \
			echo "ERROR" >> $(TEMP_DIR)/std.$@.output; \
			echo "[   J2S   ] Runtime Error(Ignore)." $<; \
		else \
			$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -cp $(OUT) P2S | $(JAVA) -jar $(SPP) >$(TEMP_DIR)/my.$@.output; \
			if [ $$? -ne 0 ]; then \
				echo "[   J2S   ] failed!" $<; \
				$(JAVA) -cp $(OUT) P2S < $< > $(TEMP_DIR)/dump.$@.spg && \
				false; \
			fi; \
			$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -jar $(PGI) > $(TEMP_DIR)/std.$@.output; \
			$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -cp $(OUT) P2S | $(JAVA) -jar $(PGI) > $(TEMP_DIR)/my.$@.output; \
			diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
			if [ $$? -eq 0 ]; then \
				echo "[   J2S   ] passed!" $<; \
				rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
			else \
				echo "[   J2S   ] failed!" $<; \
				$(JAVA) -cp $(OUT) P2S < $< > $(OUT)/dump.$@.spg && \
				false; \
			fi; \
		fi;	\
	else \
		echo "[   J2S   ] Type Error(Ignore)." $<; \
	fi

%.testtc: $(TEST_TC_DIR)/%.java
	@$(call typecheck)
	@$(JAVA) -cp $(OUT) TypeCheck < $< > $(TEMP_DIR)/my.$@.output
	@diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
	if [ $$? -eq 0 ];  then \
		echo "[TypeCheck] passed!" $<; \
	else \
		echo "[TypeCheck] failed!" $< && \
		false; \
	fi
	@-rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output

%.testmj: $(TEST_MJ_DIR)/%.java
	@$(call typecheck)
	@grep $(OkText) $(TEMP_DIR)/std.$@.output >/dev/null; \
	if [ $$? -eq 0 ]; then \
		$(JAVA) $< > $(TEMP_DIR)/std.$@.output 2>/dev/null; \
		if [ $$? -ne 0 ] ; then \
			echo "ERROR" >> $(TEMP_DIR)/std.$@.output; \
			echo "[   J2P   ] Runtime Error(Ignore)." $<; \
		else \
			$(JAVA) -cp $(OUT) J2P < $< | $(JAVA) -jar $(PGI) >$(TEMP_DIR)/my.$@.output; \
			diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
			if [ $$? -eq 0 ];  then \
				echo "[   J2P   ] passed!" $<; \
				rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
			else \
				echo "[   J2P   ] failed!" $<; \
				$(JAVA) -cp $(OUT) J2P < $< > $(TEMP_DIR)/dump.$@.pg && \
				false; \
			fi; \
		fi;\
	else \
		echo "[   J2P   ] Type Error(Ignore)." $<; \
	fi

%.testpg: $(TEST_PGI_DIR)/%.pg
	@if [ ! -d $(TEMP_DIR) ] ; then mkdir -p $(TEMP_DIR); fi
	@$(JAVA) -cp $(OUT) P2S < $< | $(JAVA) -jar $(SPP) >$(TEMP_DIR)/my.$@.output; \
	if [ $$? -ne 0 ]; then \
		echo "[   P2S   ] failed!" $<; \
		$(JAVA) -cp $(OUT) P2S < $< > $(TEMP_DIR)/dump.$@.spg && \
		false; \
	fi
	@$(JAVA) -jar $(PGI) < $< > $(TEMP_DIR)/std.$@.output
	@$(JAVA) -cp $(OUT) P2S < $< | $(JAVA) -jar $(PGI) > $(TEMP_DIR)/my.$@.output
	@diff $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
	if [ $$? -eq 0 ]; then \
		echo "[   P2S   ] passed!" $<; \
		rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
	else \
		echo "[   P2S   ] failed!" $<; \
		$(JAVA) -cp $(OUT) P2S < $< > $(OUT)/dump.$@.spg && \
		false; \
	fi
	@rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output


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

%.testkg: $(TEST_SPGI_DIR)/%.spg
	@if [ ! -d $(TEMP_DIR) ] ; then mkdir -p $(TEMP_DIR); fi
	@$(JAVA) -jar $(PGI) < $< >$(TEMP_DIR)/std.$@.output
	@$(JAVA) -cp $(OUT) S2K < $< | $(JAVA) -jar $(KGI) >$(TEMP_DIR)/my.$@.output
	@diff $(TEMP_DIR)/my.$@.output $(TEMP_DIR)/std.$@.output
	@if [ $$? -eq 0 ]; then \
		echo "[   S2K   ] passed!" $<; \
		rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output; \
	else \
		echo "[   S2K   ] failed!" $<; \
		$(JAVA) -cp $(OUT) S2K < $< > $(OUT)/dump.kg && \
		$(JAVA) -cp $(OUT) P2S < $< > $(OUT)/dump.$@.spg && \
		false; \
	fi

%.testmp: $(TEST_KGI_DIR)/%.kg
	@if [ ! -d $(TEMP_DIR) ] ; then mkdir -p $(TEMP_DIR); fi
	@$(JAVA) -jar $(KGI) < $< >$(TEMP_DIR)/std.$@.output
	@$(JAVA) -cp $(OUT) K2M < $< > $(TEMP_DIR)/dump.$@.s
	@$(SPIM) -file $(TEMP_DIR)/dump.$@.s | sed '1,5d' > $(TEMP_DIR)/my.$@.output
	@diff $(TEMP_DIR)/my.$@.output $(TEMP_DIR)/std.$@.output
	@if [ $$? -eq 0 ]; then \
		echo "[   K2M   ] passed!" $<; \
		rm $(TEMP_DIR)/std.$@.output $(TEMP_DIR)/my.$@.output $(TEMP_DIR)/dump.$@.s; \
	else \
		echo "[   K2M   ] failed!" $<; \
		$(JAVA) -cp $(OUT) K2M < $< > $(OUT)/dump.s && \
		false; \
	fi