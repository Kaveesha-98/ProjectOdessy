# 1 "../../riscv-tests/isa/rv64ui/fence_i.S"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../../riscv-tests/isa/rv64ui/fence_i.S"
# See LICENSE for license details.

#*****************************************************************************
# fence_i.S
#-----------------------------------------------------------------------------

# Test self-modifying code and the fence.i instruction.


# 1 "../../riscv-tests/env/v/riscv_test.h" 1





# 1 "../../riscv-tests/env/v/../p/riscv_test.h" 1





# 1 "../../riscv-tests/env/v/../p/../encoding.h" 1
# 7 "../../riscv-tests/env/v/../p/riscv_test.h" 2
# 7 "../../riscv-tests/env/v/riscv_test.h" 2
# 11 "../../riscv-tests/isa/rv64ui/fence_i.S" 2
# 1 "../../riscv-tests/isa/macros/scalar/test_macros.h" 1






#-----------------------------------------------------------------------
# Helper macros
#-----------------------------------------------------------------------
# 20 "../../riscv-tests/isa/macros/scalar/test_macros.h"
# We use a macro hack to simpify code generation for various numbers
# of bubble cycles.
# 36 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# RV64UI MACROS
#-----------------------------------------------------------------------

#-----------------------------------------------------------------------
# Tests for instructions with immediate operand
#-----------------------------------------------------------------------
# 92 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Tests for an instruction with register operands
#-----------------------------------------------------------------------
# 120 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Tests for an instruction with register-register operands
#-----------------------------------------------------------------------
# 214 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Test memory instructions
#-----------------------------------------------------------------------
# 347 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Test jump instructions
#-----------------------------------------------------------------------
# 376 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# RV64UF MACROS
#-----------------------------------------------------------------------

#-----------------------------------------------------------------------
# Tests floating-point instructions
#-----------------------------------------------------------------------
# 735 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Pass and fail code (assumes test num is in gp)
#-----------------------------------------------------------------------
# 747 "../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Test data section
#-----------------------------------------------------------------------
# 12 "../../riscv-tests/isa/rv64ui/fence_i.S" 2

.macro init; .endm
.text; .global _start

_start:
	j test_2

li a3, 111
lh a0, insn
lh a1, insn+2

# test I$ hit
.align 6
sh a0, 2f, t0
sh a1, 2f+2, t0
fence.i

la a5, 2f
jalr t1, a5, 0
test_2: li gp, 2; nop; li x7, ((444) & ((1 << (64 - 1) << 1) - 1)); bne a3, x7, fail;

 # test prefetcher hit
li a4, 100
1: addi a4, a4, -1
bnez a4, 1b

sh a0, 3f, t0
sh a1, 3f+2, t0
fence.i

.align 6
la a5, 3f
jalr t1, a5, 0
test_3: li gp, 3; nop; li x7, ((777) & ((1 << (64 - 1) << 1) - 1)); bne a3, x7, fail;

bne x0, gp, pass; fail: sll a0, gp, 1; 1:beqz a0, 1b; or a0, a0, 1; sb a0, -1(zero);; pass: li a0, 1; sb a0, -1(zero)

unimp

  .data
 .pushsection .tohost,"aw",@progbits; .align 6; .global tohost; tohost: .dword 0; .align 6; .global fromhost; fromhost: .dword 0; .popsection; .align 4; .global begin_signature; begin_signature:

 

insn:
  addi a3, a3, 333

2: addi a3, a3, 222
jalr a5, t1, 0

3: addi a3, a3, 555
jalr a5, t1, 0


