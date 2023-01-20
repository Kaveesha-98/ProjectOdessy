#include <stdio.h>
#include <stdlib.h>
#include "VdecodeHWTestbench.h"
#include "verilated.h"
#include "verilated_vcd_c.h"
#include <iostream>
#include <vector>
#include <deque>

struct fetchIssueEntry {
	long unsigned int PC;
    unsigned int instruction;
};

struct decodeIssueEntry {
	long unsigned int PC;
	unsigned int instruction;
	long unsigned int rs1;
	long unsigned int rs2;
	long unsigned int immediate;
};

bool operator==(const decodeIssueEntry& lhs, const decodeIssueEntry& rhs) {
	bool pcInsMatch = (lhs.instruction == rhs.instruction) && (lhs.immediate == rhs.immediate);
	// both pc and instruction must first match. more comparisions are unneccessart otherwise
	if (!pcInsMatch)
		return false;
	// not every instruction implments all fields
	switch (lhs.instruction&0b1111111) {
		case 0b0110111: // lui
			return (lhs.immediate == rhs.immediate);

		case 0b0010111: // auipc
			return (lhs.immediate == rhs.immediate);

		case 0b1101111: // jalr
			return (lhs.immediate == rhs.immediate);

		case 0b1100011: // conditional branches
			return ((lhs.immediate == rhs.immediate) && (lhs.rs1 == rhs.rs1) && (lhs.rs2 == rhs.rs2));
		
		case 0b0010011: // 64-bit immediate arithmetic
			return ((lhs.immediate == rhs.immediate) && (lhs.rs1 == rhs.rs1));

		case 0b0110011: // 64-bit register arithmetic
			return ((lhs.rs1 == rhs.rs1) && (lhs.rs2 == rhs.rs2));

		case 0b0011011: // 32-bit immediate arithmetic
			return ((lhs.immediate == rhs.immediate) && (lhs.rs1 == rhs.rs1));

		case 0b0111011: // 32-bit register arithmetic
			return ((lhs.rs1 == rhs.rs1) && (lhs.rs2 == rhs.rs2));
		default:
			return false;
	}
    return (
		(lhs.instruction == rhs.instruction) &&
		(lhs.immediate == rhs.immediate) &&
		(lhs.PC == rhs.PC) && (lhs.rs1 == rhs.rs1) && (lhs.rs2 == rhs.rs2));
}

struct branchResultEntry {
	unsigned char branchTaken;
	long unsigned int PC;
	long unsigned int targetAddress;
};

bool operator==(const branchResultEntry& lhs, const branchResultEntry& rhs) {
	return (
		(lhs.branchTaken == rhs.branchTaken) &&
		(lhs.PC == rhs.PC) && (lhs.targetAddress == rhs.targetAddress)
	);
}

void tick(int tickcount, VdecodeHWTestbench *tb, VerilatedVcdC* tfp){
	tb->eval();
	if (tfp){
		tfp->dump(tickcount*10 - 2);
	}
	tb->clock = 1;
	tb->eval();
	if(tfp){
		tfp->dump(tickcount*10);
	}
	tb->clock = 0;
	tb->eval();
	if(tfp){
		tfp->dump(tickcount*10 + 5);
		tfp->flush();
	}
}

int main(int argc, char **argv){

	/**
	 * This is the stream of instructions presented by fetch unit. 
	 * (1) Must start at 0x80000000
	 * (2) Stream does not have to be in the correct program order
	 * (3) The correct instructions issued must be in the correct
	 * 		program order(Does not have to be contigous) 
	 * (4) Better to start testing by zeroing all GPRs
	 */
	std::deque<fetchIssueEntry> instructionStream{
		//start with zeroing all registers
		{0x80000000, 0b00000000000000000000000010010011}, // zeroing x1
		{0x80000004, 0b00000000000000000000000100010011}, // zeroing x2
		{0x80000008, 0b00000000000000000000000110010011}, // zeroing x3
		{0x8000000c, 0b00000000000000000000001000010011}, // zeroing x4
		{0x80000010, 0b00000000000000000000001010010011}, // zeroing x5
		{0x80000014, 0b00000000000000000000001100010011}, // zeroing x6
		{0x80000018, 0b00000000000000000000001110010011}, // zeroing x7
		{0x8000001c, 0b00000000000000000000010000010011}, // zeroing x8
		{0x80000020, 0b00000000000000000000010010010011}, // zeroing x9
		{0x80000024, 0b00000000000000000000010100010011}, // zeroing x10
		{0x80000028, 0b00000000000000000000010110010011}, // zeroing x11
		{0x8000002c, 0b00000000000000000000011000010011}, // zeroing x12
		{0x80000030, 0b00000000000000000000011010010011}, // zeroing x13
		{0x80000034, 0b00000000000000000000011100010011}, // zeroing x14
		{0x80000038, 0b00000000000000000000011110010011}, // zeroing x15
		{0x8000003c, 0b00000000000000000000100000010011}, // zeroing x16
		{0x80000040, 0b00000000000000000000100010010011}, // zeroing x17
		{0x80000044, 0b00000000000000000000100100010011}, // zeroing x18
		{0x80000048, 0b00000000000000000000100110010011}, // zeroing x19
		{0x8000004c, 0b00000000000000000000101000010011}, // zeroing x20
		{0x80000050, 0b00000000000000000000101010010011}, // zeroing x21
		{0x80000054, 0b00000000000000000000101100010011}, // zeroing x22
		{0x80000058, 0b00000000000000000000101110010011}, // zeroing x23
		{0x8000005c, 0b00000000000000000000110000010011}, // zeroing x24
		{0x80000060, 0b00000000000000000000110010010011}, // zeroing x25
		{0x80000064, 0b00000000000000000000110100010011}, // zeroing x26
		{0x80000068, 0b00000000000000000000110110010011}, // zeroing x27
		{0x8000006c, 0b00000000000000000000111000010011}, // zeroing x28
		{0x80000070, 0b00000000000000000000111010010011}, // zeroing x29
		{0x80000074, 0b00000000000000000000111100010011}, // zeroing x30
		{0x80000078, 0b00000000000000000000111110010011}, // zeroing x31
		// test1: a taken branch followed by a stream of false pc instructions
		{0x8000007c, 0b00000000000000000000010001100011}, // beq x0, x0, 8 #0x80000084
		{0x80000080, 0b00000000100000000000000010010011}, // addi x1, x0, 8 - wrong program order
		{0x80000084, 0b00000000100000001000000010010011}  // correct program order starts here
	};

	/**
	 * The correct order in which decode unit should issue instructions to ALU. This
	 * should correspond to correct execution path
	 */
	std::deque<decodeIssueEntry> decodeStream{
		//start with zeroing all registers
		{0x80000000, 0b00000000000000000000000010010011, 0, 0, 0}, // zeroing x1
		{0x80000004, 0b00000000000000000000000100010011, 0, 0, 0}, // zeroing x2
		{0x80000008, 0b00000000000000000000000110010011, 0, 0, 0}, // zeroing x3
		{0x8000000c, 0b00000000000000000000001000010011, 0, 0, 0}, // zeroing x4
		{0x80000010, 0b00000000000000000000001010010011, 0, 0, 0}, // zeroing x5
		{0x80000014, 0b00000000000000000000001100010011, 0, 0, 0}, // zeroing x6
		{0x80000018, 0b00000000000000000000001110010011, 0, 0, 0}, // zeroing x7
		{0x8000001c, 0b00000000000000000000010000010011, 0, 0, 0}, // zeroing x8
		{0x80000020, 0b00000000000000000000010010010011, 0, 0, 0}, // zeroing x9
		{0x80000024, 0b00000000000000000000010100010011, 0, 0, 0}, // zeroing x10
		{0x80000028, 0b00000000000000000000010110010011, 0, 0, 0}, // zeroing x11
		{0x8000002c, 0b00000000000000000000011000010011, 0, 0, 0}, // zeroing x12
		{0x80000030, 0b00000000000000000000011010010011, 0, 0, 0}, // zeroing x13
		{0x80000034, 0b00000000000000000000011100010011, 0, 0, 0}, // zeroing x14
		{0x80000038, 0b00000000000000000000011110010011, 0, 0, 0}, // zeroing x15
		{0x8000003c, 0b00000000000000000000100000010011, 0, 0, 0}, // zeroing x16
		{0x80000040, 0b00000000000000000000100010010011, 0, 0, 0}, // zeroing x17
		{0x80000044, 0b00000000000000000000100100010011, 0, 0, 0}, // zeroing x18
		{0x80000048, 0b00000000000000000000100110010011, 0, 0, 0}, // zeroing x19
		{0x8000004c, 0b00000000000000000000101000010011, 0, 0, 0}, // zeroing x20
		{0x80000050, 0b00000000000000000000101010010011, 0, 0, 0}, // zeroing x21
		{0x80000054, 0b00000000000000000000101100010011, 0, 0, 0}, // zeroing x22
		{0x80000058, 0b00000000000000000000101110010011, 0, 0, 0}, // zeroing x23
		{0x8000005c, 0b00000000000000000000110000010011, 0, 0, 0}, // zeroing x24
		{0x80000060, 0b00000000000000000000110010010011, 0, 0, 0}, // zeroing x25
		{0x80000064, 0b00000000000000000000110100010011, 0, 0, 0}, // zeroing x26
		{0x80000068, 0b00000000000000000000110110010011, 0, 0, 0}, // zeroing x27
		{0x8000006c, 0b00000000000000000000111000010011, 0, 0, 0}, // zeroing x28
		{0x80000070, 0b00000000000000000000111010010011, 0, 0, 0}, // zeroing x29
		{0x80000074, 0b00000000000000000000111100010011, 0, 0, 0}, // zeroing x30
		{0x80000078, 0b00000000000000000000111110010011, 0, 0, 0}, // zeroing x31
		{0x8000007c, 0b00000000000000000000010001100011, 0, 0, 8}, // branch taken
		{0x80000084, 0b00000000100000001000000010010011, 0, 0, 8}  // next correct instruction
	};

	std::deque<branchResultEntry> branchResultStream{
		{true, 0x8000007c, 0x80000084}
	};

	unsigned tickcount = 0;

	// Call commandArgs first!
	Verilated::commandArgs(argc, argv);
	
	//Instantiate our design
	VdecodeHWTestbench *tb = new VdecodeHWTestbench;
	
	Verilated::traceEverOn(true);
	VerilatedVcdC* tfp = new VerilatedVcdC;
	tb->trace(tfp, 99);
	tfp->open("decodeHWTestbench_trace.vcd");
	
	tb -> reset = 1;
	for (int i = 0; i < 3; i++)
	{
		tick(++tickcount, tb, tfp);
	}
	tb -> reset = 0;
	tick(++tickcount, tb, tfp);

	tb -> fetchIssueIntfce_ready = 1;
	while (instructionStream.size())
	{
		tb -> fetchIssueIntfce_PC = instructionStream.at(0).PC;
		tb -> fetchIssueIntfce_instruction = instructionStream.at(0).instruction;
		// wait for 100 cycles for a responce
		for (int timeout = 100; timeout >= 0; timeout--) {
			// only one sample per cycle
			tick(++tickcount, tb, tfp);
			// checking if decode fires to alu correctly
			decodeIssueEntry curr_decode = {
				tb -> decodeIssuePort_bits_PC,
				tb -> decodeIssuePort_bits_instruction,
				tb -> decodeIssuePort_bits_rs1,
				tb -> decodeIssuePort_bits_rs2,
				tb -> decodeIssuePort_bits_immediate
			};
			if (tb -> decodeIssuePort_valid) {
				if(!decodeStream.size()) {
					std::printf("error: additional decode issue\n");
					goto exit_test;
				}

				if (!(decodeStream.at(0) == curr_decode)) {
					std::printf("decode fired wrong instruction or operands");
				} else {
					decodeStream.pop_front();
				}
			}

			// checking if decode fires branch results correctly
			branchResultEntry curr_branch_res = {
				tb -> branchResult_branchTaken,
				tb -> branchResult_PC,
				tb -> branchResult_targetAddress
			};
			if (tb -> branchResult_valid) {
				if(!branchResultStream.size()) {
					std::printf("error: additional branch result issue\n");
					goto exit_test;
				}

				if (!(branchResultStream.at(0) == curr_branch_res)) {
					std::printf("decode fired wrong branch results");
				} else {
					branchResultStream.pop_front();
				}
			}

			if(tb -> fetchIssueIntfce_issued) {
				// instruction was issued move on to next
				break;
			} else if ((tb -> fetchIssueIntfce_expected_PC != instructionStream.at(0).PC) && tb -> fetchIssueIntfce_expected_valid) {
				// instruction should not be accepted, move onto next
				break;
			} else if (!timeout) {
				std::printf("error: instruction waiting on issued timedout\n");
				goto exit_test;
			}
		}
		// moving on to next instruction
		instructionStream.pop_front();
	}

	tb -> fetchIssueIntfce_ready = 0;
	// instruction stream is issued. waiting on decodeIssuePort and branchResult
	while (branchResultStream.size() || decodeStream.size())
	{
		// wait for a response for 100 cycles
		for (int timer = 100; timer >= 0; timer--)
		{
			tick(++tickcount, tb, tfp);
			decodeIssueEntry curr_decode = {
				tb -> decodeIssuePort_bits_PC,
				tb -> decodeIssuePort_bits_instruction,
				tb -> decodeIssuePort_bits_rs1,
				tb -> decodeIssuePort_bits_rs2,
				tb -> decodeIssuePort_bits_immediate
			};
			if (tb -> decodeIssuePort_valid) {
				if(!decodeStream.size()) {
					std::printf("error: additional decode issue\n");
					goto exit_test;
				}

				if (!(decodeStream.at(0) == curr_decode)) {
					std::printf("decode fired wrong instruction or operands");
				} else {
					decodeStream.pop_front();
				}
			}

			// checking if decode fires branch results correctly
			branchResultEntry curr_branch_res = {
				tb -> branchResult_branchTaken,
				tb -> branchResult_PC,
				tb -> branchResult_targetAddress
			};
			if (tb -> branchResult_valid) {
				if(!branchResultStream.size()) {
					std::printf("error: additional branch result issue\n");
					goto exit_test;
				}

				if (!(branchResultStream.at(0) == curr_branch_res)) {
					std::printf("decode fired wrong branch results");
				} else {
					branchResultStream.pop_front();
				}
			}

			if(timer == 0) {
				std::printf("error timeout: waited on response on decodeIussePort and branchResult\n");
				goto exit_test;
			}
		}
		
	}
	
	exit_test:
	std::printf("tests finished\n");
}

