# ProjectOdessy
## Creating Multiply Unit
You can find a module that implements the integer multiply and divide instructions at src/main/scala/alu/entnM.scala. Currently the module does not cover all instructions defined in <a href="https://github.com/riscv/riscv-isa-manual/releases/download/Ratified-IMAFDQC/riscv-spec-20191213.pdf"><strong>M-extension</strong></a> of riscv.

To complete the design to implement every instruction in 1 cycle, complete lines 26 to line 47 in src/main/scala/alu/entnM.scala. The instruction that needs to be completed for each line is indicated by a comment. Lines 28 and 31 has been completed.

### Testing your multiply implementation(Only tested on Ubuntu)
```
cd cpu-test/
./verilator.sh
```
