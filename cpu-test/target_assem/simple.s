# 1 "../../riscv-tests/isa/rv64ui/simple.S"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../../riscv-tests/isa/rv64ui/simple.S"
# See LICENSE for license details.

#*****************************************************************************
# simple.S
#-----------------------------------------------------------------------------

# This is the most basic self checking test. If your simulator does not
# pass thiss then there is little chance that it will pass any of the
# more complicated self checking tests.


# 1 "../../riscv-tests/env/v/riscv_test.h" 1





# 1 "../../riscv-tests/env/v/../p/riscv_test.h" 1





# 1 "../../riscv-tests/env/v/../p/../encoding.h" 1
# 7 "../../riscv-tests/env/v/../p/riscv_test.h" 2
# 7 "../../riscv-tests/env/v/riscv_test.h" 2
# 13 "../../riscv-tests/isa/rv64ui/simple.S" 2
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
# 14 "../../riscv-tests/isa/rv64ui/simple.S" 2

.macro init; .endm
.text; .global _start

_start:
	j test_2

li a0, 1; sb a0, -1(zero)

unimp

  .data
 .pushsection .tohost,"aw",@progbits; .align 6; .global tohost; tohost: .dword 0; .align 6; .global fromhost; fromhost: .dword 0; .popsection; .align 4; .global begin_signature; begin_signature:

 


