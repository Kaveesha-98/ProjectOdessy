# 1 "../../riscv-tests/isa/rv64ui/ma_data.S"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../../riscv-tests/isa/rv64ui/ma_data.S"
# See LICENSE for license details.

#*****************************************************************************
# ma_data.S
#-----------------------------------------------------------------------------

# Test misaligned ld/st data.
# Based on rv64mi-ma_addr.S


# 1 "../../riscv-tests/env/v/riscv_test.h" 1





# 1 "../../riscv-tests/env/v/../p/riscv_test.h" 1





# 1 "../../riscv-tests/env/v/../p/../encoding.h" 1
# 7 "../../riscv-tests/env/v/../p/riscv_test.h" 2
# 7 "../../riscv-tests/env/v/riscv_test.h" 2
# 12 "../../riscv-tests/isa/rv64ui/ma_data.S" 2
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
# 13 "../../riscv-tests/isa/rv64ui/ma_data.S" 2

.macro init; .endm
.text; .global _start

_start:
	j test_2

  la s0, data
# 29 "../../riscv-tests/isa/rv64ui/ma_data.S"
# within quadword
  li gp, 1; li t1, ((-((0x0201) >> ((16)-1)) << (16)) | ((0x0201) & ((1 << (16))-1))); lh t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 2; li t1, 0x0201; lhu t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 3; li t1, ((-((0x04030201) >> ((32)-1)) << (32)) | ((0x04030201) & ((1 << (32))-1))); lw t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 4; li t1, ((-((0x05040302) >> ((32)-1)) << (32)) | ((0x05040302) & ((1 << (32))-1))); lw t2, 2(s0); bne t1, t2, fail; 1:
  li gp, 5; li t1, ((-((0x06050403) >> ((32)-1)) << (32)) | ((0x06050403) & ((1 << (32))-1))); lw t2, 3(s0); bne t1, t2, fail; 1:


  li gp, 6; li t1, 0x04030201; lwu t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 7; li t1, 0x05040302; lwu t2, 2(s0); bne t1, t2, fail; 1:
  li gp, 8; li t1, 0x06050403; lwu t2, 3(s0); bne t1, t2, fail; 1:

  li gp, 9; li t1, 0x0807060504030201; ld t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 10; li t1, 0x0908070605040302; ld t2, 2(s0); bne t1, t2, fail; 1:
  li gp, 11; li t1, 0x0a09080706050403; ld t2, 3(s0); bne t1, t2, fail; 1:
  li gp, 12; li t1, 0x0b0a090807060504; ld t2, 4(s0); bne t1, t2, fail; 1:
  li gp, 13; li t1, 0x0c0b0a0908070605; ld t2, 5(s0); bne t1, t2, fail; 1:
  li gp, 14; li t1, 0x0d0c0b0a09080706; ld t2, 6(s0); bne t1, t2, fail; 1:
  li gp, 15; li t1, 0x0e0d0c0b0a090807; ld t2, 7(s0); bne t1, t2, fail; 1:


 # octword crossing
  li gp, 16; li t1, ((-((0x201f) >> ((16)-1)) << (16)) | ((0x201f) & ((1 << (16))-1))); lh t2, 31(s0); bne t1, t2, fail; 1:
  li gp, 17; li t1, 0x201f; lhu t2, 31(s0); bne t1, t2, fail; 1:
  li gp, 18; li t1, ((-((0x201f1e1d) >> ((32)-1)) << (32)) | ((0x201f1e1d) & ((1 << (32))-1))); lw t2, 29(s0); bne t1, t2, fail; 1:
  li gp, 19; li t1, ((-((0x21201f1e) >> ((32)-1)) << (32)) | ((0x21201f1e) & ((1 << (32))-1))); lw t2, 30(s0); bne t1, t2, fail; 1:
  li gp, 20; li t1, ((-((0x2221201f) >> ((32)-1)) << (32)) | ((0x2221201f) & ((1 << (32))-1))); lw t2, 31(s0); bne t1, t2, fail; 1:


  li gp, 21; li t1, 0x201f1e1d; lwu t2, 29(s0); bne t1, t2, fail; 1:
  li gp, 22; li t1, 0x21201f1e; lwu t2, 30(s0); bne t1, t2, fail; 1:
  li gp, 23; li t1, 0x2221201f; lwu t2, 31(s0); bne t1, t2, fail; 1:

  li gp, 24; li t1, 0x201f1e1d1c1b1a19; ld t2, 25(s0); bne t1, t2, fail; 1:
  li gp, 25; li t1, 0x21201f1e1d1c1b1a; ld t2, 26(s0); bne t1, t2, fail; 1:
  li gp, 26; li t1, 0x2221201f1e1d1c1b; ld t2, 27(s0); bne t1, t2, fail; 1:
  li gp, 27; li t1, 0x232221201f1e1d1c; ld t2, 28(s0); bne t1, t2, fail; 1:
  li gp, 28; li t1, 0x24232221201f1e1d; ld t2, 29(s0); bne t1, t2, fail; 1:
  li gp, 29; li t1, 0x2524232221201f1e; ld t2, 30(s0); bne t1, t2, fail; 1:
  li gp, 30; li t1, 0x262524232221201f; ld t2, 31(s0); bne t1, t2, fail; 1:


 # cacheline crossing
  li gp, 31; li t1, ((-((0x403f) >> ((16)-1)) << (16)) | ((0x403f) & ((1 << (16))-1))); lh t2, 63(s0); bne t1, t2, fail; 1:
  li gp, 32; li t1, 0x403f; lhu t2, 63(s0); bne t1, t2, fail; 1:
  li gp, 33; li t1, ((-((0x403f3e3d) >> ((32)-1)) << (32)) | ((0x403f3e3d) & ((1 << (32))-1))); lw t2, 61(s0); bne t1, t2, fail; 1:
  li gp, 34; li t1, ((-((0x41403f3e) >> ((32)-1)) << (32)) | ((0x41403f3e) & ((1 << (32))-1))); lw t2, 62(s0); bne t1, t2, fail; 1:
  li gp, 35; li t1, ((-((0x4241403f) >> ((32)-1)) << (32)) | ((0x4241403f) & ((1 << (32))-1))); lw t2, 63(s0); bne t1, t2, fail; 1:


  li gp, 36; li t1, 0x403f3e3d; lwu t2, 61(s0); bne t1, t2, fail; 1:
  li gp, 37; li t1, 0x41403f3e; lwu t2, 62(s0); bne t1, t2, fail; 1:
  li gp, 38; li t1, 0x4241403f; lwu t2, 63(s0); bne t1, t2, fail; 1:

  li gp, 39; li t1, 0x403f3e3d3c3b3a39; ld t2, 57(s0); bne t1, t2, fail; 1:
  li gp, 40; li t1, 0x41403f3e3d3c3b3a; ld t2, 58(s0); bne t1, t2, fail; 1:
  li gp, 41; li t1, 0x4241403f3e3d3c3b; ld t2, 59(s0); bne t1, t2, fail; 1:
  li gp, 42; li t1, 0x434241403f3e3d3c; ld t2, 60(s0); bne t1, t2, fail; 1:
  li gp, 43; li t1, 0x44434241403f3e3d; ld t2, 61(s0); bne t1, t2, fail; 1:
  li gp, 44; li t1, 0x4544434241403f3e; ld t2, 62(s0); bne t1, t2, fail; 1:
  li gp, 45; li t1, 0x464544434241403f; ld t2, 63(s0); bne t1, t2, fail; 1:
# 102 "../../riscv-tests/isa/rv64ui/ma_data.S"
 # within quadword
  li gp, 46; li t1, ((-((0x8180) >> ((16)-1)) << (16)) | ((0x8180) & ((1 << (16))-1))); sh t1, 1(s0); lh t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 47; li t1, 0x8382; sh t1, 1(s0); lhu t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 48; li t1, ((-((0x87868584) >> ((32)-1)) << (32)) | ((0x87868584) & ((1 << (32))-1))); sw t1, 1(s0); lw t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 49; li t1, ((-((0x8b8a8988) >> ((32)-1)) << (32)) | ((0x8b8a8988) & ((1 << (32))-1))); sw t1, 2(s0); lw t2, 2(s0); bne t1, t2, fail; 1:
  li gp, 50; li t1, ((-((0x8f8e8d8c) >> ((32)-1)) << (32)) | ((0x8f8e8d8c) & ((1 << (32))-1))); sw t1, 3(s0); lw t2, 3(s0); bne t1, t2, fail; 1:


  li gp, 51; li t1, 0x93929190; sw t1, 1(s0); lwu t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 52; li t1, 0x97969594; sw t1, 2(s0); lwu t2, 2(s0); bne t1, t2, fail; 1:
  li gp, 53; li t1, 0x9b9a9998; sw t1, 3(s0); lwu t2, 3(s0); bne t1, t2, fail; 1:

  li gp, 54; li t1, 0xa3a2a1a09f9e9d9c; sd t1, 1(s0); ld t2, 1(s0); bne t1, t2, fail; 1:
  li gp, 55; li t1, 0xabaaa9a8a7a6a5a4; sd t1, 2(s0); ld t2, 2(s0); bne t1, t2, fail; 1:
  li gp, 56; li t1, 0xb3b2b1b0afaeadac; sd t1, 3(s0); ld t2, 3(s0); bne t1, t2, fail; 1:
  li gp, 57; li t1, 0xbbbab9b8b7b6b5b4; sd t1, 4(s0); ld t2, 4(s0); bne t1, t2, fail; 1:
  li gp, 58; li t1, 0xc3c2c1c0bfbebdbc; sd t1, 5(s0); ld t2, 5(s0); bne t1, t2, fail; 1:
  li gp, 59; li t1, 0xcbcac9c8c7c6c5c4; sd t1, 6(s0); ld t2, 6(s0); bne t1, t2, fail; 1:
  li gp, 60; li t1, 0xd3d2d1d0cfcecdcc; sd t1, 7(s0); ld t2, 7(s0); bne t1, t2, fail; 1:


 # octword crossing
  li gp, 61; li t1, ((-((0xd5d4) >> ((16)-1)) << (16)) | ((0xd5d4) & ((1 << (16))-1))); sh t1, 31(s0); lh t2, 31(s0); bne t1, t2, fail; 1:
  li gp, 62; li t1, 0xd7d6; sh t1, 31(s0); lhu t2, 31(s0); bne t1, t2, fail; 1:
  li gp, 63; li t1, ((-((0xdbdad9d8) >> ((32)-1)) << (32)) | ((0xdbdad9d8) & ((1 << (32))-1))); sw t1, 29(s0); lw t2, 29(s0); bne t1, t2, fail; 1:
  li gp, 64; li t1, ((-((0xdfdedddc) >> ((32)-1)) << (32)) | ((0xdfdedddc) & ((1 << (32))-1))); sw t1, 30(s0); lw t2, 30(s0); bne t1, t2, fail; 1:
  li gp, 65; li t1, ((-((0xe3e2e1e0) >> ((32)-1)) << (32)) | ((0xe3e2e1e0) & ((1 << (32))-1))); sw t1, 31(s0); lw t2, 31(s0); bne t1, t2, fail; 1:


  li gp, 66; li t1, 0xe7e6e5e4; sw t1, 29(s0); lwu t2, 29(s0); bne t1, t2, fail; 1:
  li gp, 67; li t1, 0xebeae9e8; sw t1, 30(s0); lwu t2, 30(s0); bne t1, t2, fail; 1:
  li gp, 68; li t1, 0xefeeedec; sw t1, 31(s0); lwu t2, 31(s0); bne t1, t2, fail; 1:

  li gp, 69; li t1, 0xf7f6f5f4f3f2f1f0; sd t1, 25(s0); ld t2, 25(s0); bne t1, t2, fail; 1:
  li gp, 70; li t1, 0xfffefdfcfbfaf9f8; sd t1, 26(s0); ld t2, 26(s0); bne t1, t2, fail; 1:
  li gp, 71; li t1, 0x0706050403020100; sd t1, 27(s0); ld t2, 27(s0); bne t1, t2, fail; 1:
  li gp, 72; li t1, 0x0f0e0d0c0b0a0908; sd t1, 28(s0); ld t2, 28(s0); bne t1, t2, fail; 1:
  li gp, 73; li t1, 0x1716151413121110; sd t1, 29(s0); ld t2, 29(s0); bne t1, t2, fail; 1:
  li gp, 74; li t1, 0x1f1e1d1c1b1a1918; sd t1, 30(s0); ld t2, 30(s0); bne t1, t2, fail; 1:
  li gp, 75; li t1, 0x2726252423222120; sd t1, 31(s0); ld t2, 31(s0); bne t1, t2, fail; 1:


 # cacheline crossing
  li gp, 76; li t1, ((-((0x3534) >> ((16)-1)) << (16)) | ((0x3534) & ((1 << (16))-1))); sh t1, 63(s0); lh t2, 63(s0); bne t1, t2, fail; 1:
  li gp, 77; li t1, 0x3736; sh t1, 63(s0); lhu t2, 63(s0); bne t1, t2, fail; 1:
  li gp, 78; li t1, ((-((0x3b3a3938) >> ((32)-1)) << (32)) | ((0x3b3a3938) & ((1 << (32))-1))); sw t1, 61(s0); lw t2, 61(s0); bne t1, t2, fail; 1:
  li gp, 79; li t1, ((-((0x3f3e3d3c) >> ((32)-1)) << (32)) | ((0x3f3e3d3c) & ((1 << (32))-1))); sw t1, 62(s0); lw t2, 62(s0); bne t1, t2, fail; 1:
  li gp, 80; li t1, ((-((0x43424140) >> ((32)-1)) << (32)) | ((0x43424140) & ((1 << (32))-1))); sw t1, 63(s0); lw t2, 63(s0); bne t1, t2, fail; 1:


  li gp, 81; li t1, 0x47464544; sw t1, 61(s0); lwu t2, 61(s0); bne t1, t2, fail; 1:
  li gp, 82; li t1, 0x4b4a4948; sw t1, 62(s0); lwu t2, 62(s0); bne t1, t2, fail; 1:
  li gp, 83; li t1, 0x4f4e4d4c; sw t1, 63(s0); lwu t2, 63(s0); bne t1, t2, fail; 1:

  li gp, 84; li t1, 0x5756555453525150; sd t1, 57(s0); ld t2, 57(s0); bne t1, t2, fail; 1:
  li gp, 85; li t1, 0x5f5e5d5c5b5a5958; sd t1, 58(s0); ld t2, 58(s0); bne t1, t2, fail; 1:
  li gp, 86; li t1, 0x6766656463626160; sd t1, 59(s0); ld t2, 59(s0); bne t1, t2, fail; 1:
  li gp, 87; li t1, 0x6f6e6d6c6b6a6968; sd t1, 60(s0); ld t2, 60(s0); bne t1, t2, fail; 1:
  li gp, 88; li t1, 0x7776757473727170; sd t1, 61(s0); ld t2, 61(s0); bne t1, t2, fail; 1:
  li gp, 89; li t1, 0x7f7e7d7c7b7a7978; sd t1, 62(s0); ld t2, 62(s0); bne t1, t2, fail; 1:
  li gp, 90; li t1, 0x8786858483828180; sd t1, 63(s0); ld t2, 63(s0); bne t1, t2, fail; 1:
# 176 "../../riscv-tests/isa/rv64ui/ma_data.S"
 # within quadword
  li gp, 91; li t1, 0x9998; li t2, ((-((0x98) >> ((8)-1)) << (8)) | ((0x98) & ((1 << (8))-1))); sh t1, 1(s0); lb t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 92; li t1, 0x9b9a; li t2, ((-((0x9b) >> ((8)-1)) << (8)) | ((0x9b) & ((1 << (8))-1))); sh t1, 1(s0); lb t3, 2(s0); bne t2, t3, fail; 1:
  li gp, 93; li t1, 0x9d9c; li t2, 0x9c; sh t1, 1(s0); lbu t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 94; li t1, 0x9f9e; li t2, 0x9f; sh t1, 1(s0); lbu t3, 2(s0); bne t2, t3, fail; 1:
  li gp, 95; li t1, 0xa3a2a1a0; li t2, ((-((0xa0) >> ((8)-1)) << (8)) | ((0xa0) & ((1 << (8))-1))); sw t1, 1(s0); lb t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 96; li t1, 0xa7a6a5a4; li t2, 0xa5; sw t1, 2(s0); lbu t3, 3(s0); bne t2, t3, fail; 1:
  li gp, 97; li t1, 0xabaaa9a8; li t2, ((-((0xaaa9) >> ((16)-1)) << (16)) | ((0xaaa9) & ((1 << (16))-1))); sw t1, 3(s0); lh t3, 4(s0); bne t2, t3, fail; 1:
  li gp, 98; li t1, 0xafaeadac; li t2, 0xafae; sw t1, 3(s0); lhu t3, 5(s0); bne t2, t3, fail; 1:


  li gp, 99; li t1, 0xb7b6b5b4b3b2b1b0; li t2, ((-((0xb6) >> ((8)-1)) << (8)) | ((0xb6) & ((1 << (8))-1))); sd t1, 1(s0); lb t3, 7(s0); bne t2, t3, fail; 1:
  li gp, 100; li t1, 0xbfbebdbcbbbab9b8; li t2, 0xb9; sd t1, 2(s0); lbu t3, 3(s0); bne t2, t3, fail; 1:
  li gp, 101; li t1, 0xc7c6c5c4c3c2c1c0; li t2, ((-((0xc7c6) >> ((16)-1)) << (16)) | ((0xc7c6) & ((1 << (16))-1))); sd t1, 3(s0); lh t3, 9(s0); bne t2, t3, fail; 1:
  li gp, 102; li t1, 0xcfcecdcccbcac9c8; li t2, 0xcac9; sd t1, 4(s0); lhu t3, 5(s0); bne t2, t3, fail; 1:
  li gp, 103; li t1, 0xd7d6d5d4d3d2d1d0; li t2, ((-((0xd7d6d5d4) >> ((32)-1)) << (32)) | ((0xd7d6d5d4) & ((1 << (32))-1))); sd t1, 5(s0); lw t3, 9(s0); bne t2, t3, fail; 1:
  li gp, 104; li t1, 0xdfdedddcdbdad9d8; li t2, ((-((0xdddcdbda) >> ((32)-1)) << (32)) | ((0xdddcdbda) & ((1 << (32))-1))); sd t1, 6(s0); lw t3, 8(s0); bne t2, t3, fail; 1:
  li gp, 105; li t1, 0xe7e6e5e4e3e2e1e0; li t2, 0xe4e3e2e1; sd t1, 7(s0); lwu t3, 8(s0); bne t2, t3, fail; 1:


 # octword crossing
  li gp, 106; li t1, 0xe9e8; li t2, ((-((0xe8) >> ((8)-1)) << (8)) | ((0xe8) & ((1 << (8))-1))); sh t1, 31(s0); lb t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 107; li t1, 0xebea; li t2, ((-((0xeb) >> ((8)-1)) << (8)) | ((0xeb) & ((1 << (8))-1))); sh t1, 31(s0); lb t3, 32(s0); bne t2, t3, fail; 1:
  li gp, 108; li t1, 0xedec; li t2, 0xec; sh t1, 31(s0); lbu t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 109; li t1, 0xefee; li t2, 0xef; sh t1, 31(s0); lbu t3, 32(s0); bne t2, t3, fail; 1:
  li gp, 110; li t1, 0xf3f2f1f0; li t2, ((-((0xf0) >> ((8)-1)) << (8)) | ((0xf0) & ((1 << (8))-1))); sw t1, 29(s0); lb t3, 29(s0); bne t2, t3, fail; 1:
  li gp, 111; li t1, 0xf7f6f5f4; li t2, 0xf6; sw t1, 30(s0); lbu t3, 32(s0); bne t2, t3, fail; 1:
  li gp, 112; li t1, 0xfbfaf9f8; li t2, ((-((0xfbfa) >> ((16)-1)) << (16)) | ((0xfbfa) & ((1 << (16))-1))); sw t1, 29(s0); lh t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 113; li t1, 0xfffefdfc; li t2, 0xfdfc; sw t1, 31(s0); lhu t3, 31(s0); bne t2, t3, fail; 1:


  li gp, 114; li t1, 0x0706050403020100; li t2, ((-((0x07) >> ((8)-1)) << (8)) | ((0x07) & ((1 << (8))-1))); sd t1, 25(s0); lb t3, 32(s0); bne t2, t3, fail; 1:
  li gp, 115; li t1, 0x0f0e0d0c0b0a0908; li t2, 0x0f; sd t1, 26(s0); lbu t3, 33(s0); bne t2, t3, fail; 1:
  li gp, 116; li t1, 0x1716151413121110; li t2, ((-((0x1514) >> ((16)-1)) << (16)) | ((0x1514) & ((1 << (16))-1))); sd t1, 27(s0); lh t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 117; li t1, 0x1f1e1d1c1b1a1918; li t2, 0x1c1b; sd t1, 28(s0); lhu t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 118; li t1, 0x2726252423222120; li t2, ((-((0x23222120) >> ((32)-1)) << (32)) | ((0x23222120) & ((1 << (32))-1))); sd t1, 29(s0); lw t3, 29(s0); bne t2, t3, fail; 1:
  li gp, 119; li t1, 0x2f2e2d2c2b2a2928; li t2, ((-((0x2b2a2928) >> ((32)-1)) << (32)) | ((0x2b2a2928) & ((1 << (32))-1))); sd t1, 30(s0); lw t3, 30(s0); bne t2, t3, fail; 1:
  li gp, 120; li t1, 0x3736353433323130; li t2, 0x33323130; sd t1, 31(s0); lwu t3, 31(s0); bne t2, t3, fail; 1:


 # cacheline crossing
  li gp, 121; li t1, 0x4948; li t2, ((-((0x48) >> ((8)-1)) << (8)) | ((0x48) & ((1 << (8))-1))); sh t1, 63(s0); lb t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 122; li t1, 0x4b4a; li t2, ((-((0x4b) >> ((8)-1)) << (8)) | ((0x4b) & ((1 << (8))-1))); sh t1, 63(s0); lb t3, 64(s0); bne t2, t3, fail; 1:
  li gp, 123; li t1, 0x4d4c; li t2, 0x4c; sh t1, 63(s0); lbu t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 124; li t1, 0x4f4e; li t2, 0x4f; sh t1, 63(s0); lbu t3, 64(s0); bne t2, t3, fail; 1:
  li gp, 125; li t1, 0x53525150; li t2, ((-((0x50) >> ((8)-1)) << (8)) | ((0x50) & ((1 << (8))-1))); sw t1, 61(s0); lb t3, 61(s0); bne t2, t3, fail; 1:
  li gp, 126; li t1, 0x57565554; li t2, 0x56; sw t1, 62(s0); lbu t3, 64(s0); bne t2, t3, fail; 1:
  li gp, 127; li t1, 0x5b5a5958; li t2, ((-((0x5b5a) >> ((16)-1)) << (16)) | ((0x5b5a) & ((1 << (16))-1))); sw t1, 61(s0); lh t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 128; li t1, 0x5f5e5d5c; li t2, 0x5d5c; sw t1, 63(s0); lhu t3, 63(s0); bne t2, t3, fail; 1:


  li gp, 129; li t1, 0x6766656463626160; li t2, ((-((0x67) >> ((8)-1)) << (8)) | ((0x67) & ((1 << (8))-1))); sd t1, 57(s0); lb t3, 64(s0); bne t2, t3, fail; 1:
  li gp, 130; li t1, 0x6f6e6d6c6b6a6968; li t2, 0x6f; sd t1, 58(s0); lbu t3, 65(s0); bne t2, t3, fail; 1:
  li gp, 131; li t1, 0x7776757473727170; li t2, ((-((0x7574) >> ((16)-1)) << (16)) | ((0x7574) & ((1 << (16))-1))); sd t1, 59(s0); lh t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 132; li t1, 0x7f7e7d7c7b7a7978; li t2, 0x7c7b; sd t1, 60(s0); lhu t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 133; li t1, 0x8786858483828180; li t2, ((-((0x83828180) >> ((32)-1)) << (32)) | ((0x83828180) & ((1 << (32))-1))); sd t1, 61(s0); lw t3, 61(s0); bne t2, t3, fail; 1:
  li gp, 134; li t1, 0x8f8e8d8c8b8a8988; li t2, ((-((0x8b8a8988) >> ((32)-1)) << (32)) | ((0x8b8a8988) & ((1 << (32))-1))); sd t1, 62(s0); lw t3, 62(s0); bne t2, t3, fail; 1:
  li gp, 135; li t1, 0x9796959493929190; li t2, 0x93929190; sd t1, 63(s0); lwu t3, 63(s0); bne t2, t3, fail; 1:
# 275 "../../riscv-tests/isa/rv64ui/ma_data.S"
 # within quadword
  li gp, 136; li t1, 0x98; li t2, ((-((0xb898) >> ((16)-1)) << (16)) | ((0xb898) & ((1 << (16))-1))); sb t1, 1(s0); lh t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 137; li t1, 0x99; li t2, 0x9998; sb t1, 2(s0); lhu t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 138; li t1, 0x9b9a; li t2, ((-((0xc8c09b9a) >> ((32)-1)) << (32)) | ((0xc8c09b9a) & ((1 << (32))-1))); sh t1, 1(s0); lw t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 139; li t1, 0x9d9c; li t2, ((-((0xd09d9c9b) >> ((32)-1)) << (32)) | ((0xd09d9c9b) & ((1 << (32))-1))); sh t1, 3(s0); lw t3, 2(s0); bne t2, t3, fail; 1:
  li gp, 140; li t1, 0x9f9e; li t2, ((-((0x9f9e9d9c) >> ((32)-1)) << (32)) | ((0x9f9e9d9c) & ((1 << (32))-1))); sh t1, 5(s0); lw t3, 3(s0); bne t2, t3, fail; 1:


  li gp, 141; li t1, 0xa0; li t2, 0x9d9ca09a; sb t1, 2(s0); lwu t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 142; li t1, 0xa2a1; li t2, 0x9ea2a1a0; sh t1, 3(s0); lwu t3, 2(s0); bne t2, t3, fail; 1:
  li gp, 143; li t1, 0xa4a3; li t2, 0xa4a3a2a1; sh t1, 5(s0); lwu t3, 3(s0); bne t2, t3, fail; 1:

  li gp, 144; li t1, 0xa5; li t2, 0xe1e0a4a3a2a1a59a; sb t1, 2(s0); ld t3, 1(s0); bne t2, t3, fail; 1:
  li gp, 145; li t1, 0xa7a6; li t2, 0xe2a7a6a4a3a2a1a5; sh t1, 7(s0); ld t3, 2(s0); bne t2, t3, fail; 1:
  li gp, 146; li t1, 0xa9a8; li t2, 0xa9a8a7a6a4a3a2a1; sh t1, 9(s0); ld t3, 3(s0); bne t2, t3, fail; 1:
  li gp, 147; li t1, 0xadacabaa; li t2, 0xe4a9a8adacabaaa2; sw t1, 5(s0); ld t3, 4(s0); bne t2, t3, fail; 1:
  li gp, 148; li t1, 0xb1b0afae; li t2, 0xe5e4b1b0afaeabaa; sw t1, 7(s0); ld t3, 5(s0); bne t2, t3, fail; 1:
  li gp, 149; li t1, 0xb5b4b3b2; li t2, 0xe6b5b4b3b2afaeab; sw t1, 9(s0); ld t3, 6(s0); bne t2, t3, fail; 1:
  li gp, 150; li t1, 0xb9b8b7b6; li t2, 0xb9b8b7b6b3b2afae; sw t1, 11(s0); ld t3, 7(s0); bne t2, t3, fail; 1:


 # octword crossing
  li gp, 151; li t1, 0xba; li t2, ((-((0x31ba) >> ((16)-1)) << (16)) | ((0x31ba) & ((1 << (16))-1))); sb t1, 31(s0); lh t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 152; li t1, 0xbb; li t2, 0xbbba; sb t1, 32(s0); lhu t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 153; li t1, 0xbdbc; li t2, ((-((0x32bbbdbc) >> ((32)-1)) << (32)) | ((0x32bbbdbc) & ((1 << (32))-1))); sh t1, 30(s0); lw t3, 30(s0); bne t2, t3, fail; 1:
  li gp, 154; li t1, 0xbfbe; li t2, ((-((0x32bfbebc) >> ((32)-1)) << (32)) | ((0x32bfbebc) & ((1 << (32))-1))); sh t1, 31(s0); lw t3, 30(s0); bne t2, t3, fail; 1:
  li gp, 155; li t1, 0xc1c0; li t2, ((-((0xc1c0bebc) >> ((32)-1)) << (32)) | ((0xc1c0bebc) & ((1 << (32))-1))); sh t1, 32(s0); lw t3, 30(s0); bne t2, t3, fail; 1:


  li gp, 156; li t1, 0xc2; li t2, 0x33c1c2be; sb t1, 32(s0); lwu t3, 31(s0); bne t2, t3, fail; 1:
  li gp, 157; li t1, 0xc4c3; li t2, 0xc4c3bc20; sh t1, 31(s0); lwu t3, 29(s0); bne t2, t3, fail; 1:
  li gp, 158; li t1, 0xc6c5; li t2, 0xc6c5c3bc; sh t1, 32(s0); lwu t3, 30(s0); bne t2, t3, fail; 1:

  li gp, 159; li t1, 0xc7; li t2, 0xc7c3bc2018100800; sb t1, 32(s0); ld t3, 25(s0); bne t2, t3, fail; 1:
  li gp, 160; li t1, 0xc9c8; li t2, 0xc6c9c8bc20181008; sh t1, 31(s0); ld t3, 26(s0); bne t2, t3, fail; 1:
  li gp, 161; li t1, 0xcbca; li t2, 0x33c6cbcabc201810; sh t1, 31(s0); ld t3, 27(s0); bne t2, t3, fail; 1:
  li gp, 162; li t1, 0xcfcecdcc; li t2, 0xcfcecdcccabc2018; sw t1, 32(s0); ld t3, 28(s0); bne t2, t3, fail; 1:
  li gp, 163; li t1, 0xd3d2d1d0; li t2, 0x35cfd3d2d1d0bc20; sw t1, 31(s0); ld t3, 29(s0); bne t2, t3, fail; 1:
  li gp, 164; li t1, 0xd7d6d5d4; li t2, 0x3635cfd3d7d6d5d4; sw t1, 30(s0); ld t3, 30(s0); bne t2, t3, fail; 1:
  li gp, 165; li t1, 0xdbdad9d8; li t2, 0x373635cfd3d7dbda; sw t1, 29(s0); ld t3, 31(s0); bne t2, t3, fail; 1:


 # cacheline crossing
  li gp, 166; li t1, 0xdc; li t2, ((-((0x91dc) >> ((16)-1)) << (16)) | ((0x91dc) & ((1 << (16))-1))); sb t1, 63(s0); lh t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 167; li t1, 0xdd; li t2, 0xdddc; sb t1, 64(s0); lhu t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 168; li t1, 0xdfde; li t2, ((-((0x92dddfde) >> ((32)-1)) << (32)) | ((0x92dddfde) & ((1 << (32))-1))); sh t1, 62(s0); lw t3, 62(s0); bne t2, t3, fail; 1:
  li gp, 169; li t1, 0xe1e0; li t2, ((-((0x92e1e0de) >> ((32)-1)) << (32)) | ((0x92e1e0de) & ((1 << (32))-1))); sh t1, 63(s0); lw t3, 62(s0); bne t2, t3, fail; 1:
  li gp, 170; li t1, 0xe3e2; li t2, ((-((0xe3e2e0de) >> ((32)-1)) << (32)) | ((0xe3e2e0de) & ((1 << (32))-1))); sh t1, 64(s0); lw t3, 62(s0); bne t2, t3, fail; 1:


  li gp, 171; li t1, 0xe4; li t2, 0x93e3e4e0; sb t1, 64(s0); lwu t3, 63(s0); bne t2, t3, fail; 1:
  li gp, 172; li t1, 0xe6e5; li t2, 0xe6e5de80; sh t1, 63(s0); lwu t3, 61(s0); bne t2, t3, fail; 1:
  li gp, 173; li t1, 0xe8e7; li t2, 0xe8e7e5de; sh t1, 64(s0); lwu t3, 62(s0); bne t2, t3, fail; 1:

  li gp, 174; li t1, 0xe9; li t2, 0xe9e5de8078706860; sb t1, 64(s0); ld t3, 57(s0); bne t2, t3, fail; 1:
  li gp, 175; li t1, 0xebea; li t2, 0xe8ebeade80787068; sh t1, 63(s0); ld t3, 58(s0); bne t2, t3, fail; 1:
  li gp, 176; li t1, 0xedec; li t2, 0x93e8edecde807870; sh t1, 63(s0); ld t3, 59(s0); bne t2, t3, fail; 1:
  li gp, 177; li t1, 0xf1f0efee; li t2, 0xf1f0efeeecde8078; sw t1, 64(s0); ld t3, 60(s0); bne t2, t3, fail; 1:
  li gp, 178; li t1, 0xf5f4f3f2; li t2, 0x95f1f5f4f3f2de80; sw t1, 63(s0); ld t3, 61(s0); bne t2, t3, fail; 1:
  li gp, 179; li t1, 0xf9f8f7f6; li t2, 0x9695f1f5f9f8f7f6; sw t1, 62(s0); ld t3, 62(s0); bne t2, t3, fail; 1:
  li gp, 180; li t1, 0xfdfcfbfa; li t2, 0x979695f1f5f9fdfc; sw t1, 61(s0); ld t3, 63(s0); bne t2, t3, fail; 1:


  bne x0, gp, pass; fail: sll a0, gp, 1; 1:beqz a0, 1b; or a0, a0, 1; sb a0, -1(zero);; pass: li a0, 1; sb a0, -1(zero)

unimp

  .data
 .pushsection .tohost,"aw",@progbits; .align 6; .global tohost; tohost: .dword 0; .align 6; .global fromhost; fromhost: .dword 0; .popsection; .align 4; .global begin_signature; begin_signature:

data:
  .align 3

.word 0x03020100
.word 0x07060504
.word 0x0b0a0908
.word 0x0f0e0d0c
.word 0x13121110
.word 0x17161514
.word 0x1b1a1918
.word 0x1f1e1d1c
.word 0x23222120
.word 0x27262524
.word 0x2b2a2928
.word 0x2f2e2d2c
.word 0x33323130
.word 0x37363534
.word 0x3b3a3938
.word 0x3f3e3d3c

.word 0x43424140
.word 0x47464544
.word 0x4b4a4948
.word 0x4f4e4d4c
.word 0x53525150
.word 0x57565554
.word 0x5b5a5958
.word 0x5f5e5d5c
.word 0x63626160
.word 0x67666564
.word 0x6b6a6968
.word 0x6f6e6d6c
.word 0x73727170
.word 0x77767574
.word 0x7b7a7978
.word 0x7f7e7d7c

.fill 0xff, 1, 80


 


