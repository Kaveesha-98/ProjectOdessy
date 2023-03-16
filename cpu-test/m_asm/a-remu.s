# 0 "../../../riscv-tests/isa/rv64um/remu.S"
# 0 "<built-in>"
# 0 "<command-line>"
# 1 "../../../riscv-tests/isa/rv64um/remu.S"
# See LICENSE for license details.

#*****************************************************************************
# remu.S
#-----------------------------------------------------------------------------

# Test remu instruction.


# 1 "../../../riscv-tests/env/v/riscv_test.h" 1





# 1 "../../../riscv-tests/env/v/../p/riscv_test.h" 1





# 1 "../../../riscv-tests/env/v/../p/../encoding.h" 1
# 7 "../../../riscv-tests/env/v/../p/riscv_test.h" 2
# 7 "../../../riscv-tests/env/v/riscv_test.h" 2
# 11 "../../../riscv-tests/isa/rv64um/remu.S" 2
# 1 "../../../riscv-tests/isa/macros/scalar/test_macros.h" 1






#-----------------------------------------------------------------------
# Helper macros
#-----------------------------------------------------------------------
# 20 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
# We use a macro hack to simpify code generation for various numbers
# of bubble cycles.
# 36 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# RV64UI MACROS
#-----------------------------------------------------------------------

#-----------------------------------------------------------------------
# Tests for instructions with immediate operand
#-----------------------------------------------------------------------
# 92 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Tests for an instruction with register operands
#-----------------------------------------------------------------------
# 120 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Tests for an instruction with register-register operands
#-----------------------------------------------------------------------
# 214 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Test memory instructions
#-----------------------------------------------------------------------
# 347 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Test jump instructions
#-----------------------------------------------------------------------
# 376 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# RV64UF MACROS
#-----------------------------------------------------------------------

#-----------------------------------------------------------------------
# Tests floating-point instructions
#-----------------------------------------------------------------------
# 735 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Pass and fail code (assumes test num is in gp)
#-----------------------------------------------------------------------
# 747 "../../../riscv-tests/isa/macros/scalar/test_macros.h"
#-----------------------------------------------------------------------
# Test data section
#-----------------------------------------------------------------------
# 12 "../../../riscv-tests/isa/rv64um/remu.S" 2

.macro init; .endm
.text; .global _start

_start:

  #-------------------------------------------------------------
  # Arithmetic tests
  #-------------------------------------------------------------

  test_2: li gp, 2; li x1, ((20) & ((1 << (64 - 1) << 1) - 1)); li x2, ((6) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((2) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;
  test_3: li gp, 3; li x1, ((-20) & ((1 << (64 - 1) << 1) - 1)); li x2, ((6) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((2) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;
  test_4: li gp, 4; li x1, ((20) & ((1 << (64 - 1) << 1) - 1)); li x2, ((-6) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((20) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;
  test_5: li gp, 5; li x1, ((-20) & ((1 << (64 - 1) << 1) - 1)); li x2, ((-6) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((-20) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;

  test_6: li gp, 6; li x1, ((-1<<63) & ((1 << (64 - 1) << 1) - 1)); li x2, ((1) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((0) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;
  test_7: li gp, 7; li x1, ((-1<<63) & ((1 << (64 - 1) << 1) - 1)); li x2, ((-1) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((-1<<63) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;

  test_8: li gp, 8; li x1, ((-1<<63) & ((1 << (64 - 1) << 1) - 1)); li x2, ((0) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((-1<<63) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;
  test_9: li gp, 9; li x1, ((1) & ((1 << (64 - 1) << 1) - 1)); li x2, ((0) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((1) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;
  test_10: li gp, 10; li x1, ((0) & ((1 << (64 - 1) << 1) - 1)); li x2, ((0) & ((1 << (64 - 1) << 1) - 1)); remu x14, x1, x2;; li x7, ((0) & ((1 << (64 - 1) << 1) - 1)); bne x14, x7, fail;;

  bne x0, gp, pass; fail: sll a0, gp, 1; 1:beqz a0, 1b; or a0, a0, 1; sb a0, -1(zero);; pass: li a0, 1; sb a0, -1(zero)

unimp

  .data
 .pushsection .tohost,"aw",@progbits; .align 6; .global tohost; tohost: .dword 0; .align 6; .global fromhost; fromhost: .dword 0; .popsection; .align 4; .global begin_signature; begin_signature:

 


