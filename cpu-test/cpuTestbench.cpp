#include <stdio.h>
#include <stdlib.h>
#include "VcpuTestbench.h"
#include "verilated.h"
#include "verilated_vcd_c.h"
#include <fstream>
#include <iterator>
#include <vector>
#include <iostream>
#include <string>

using namespace std;

void tick(int tickcount, VcpuTestbench *tb, VerilatedVcdC* tfp){
	tb->eval();
	if(tfp){
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

	unsigned tickcount = 0;

	// Call commandArgs first!
	Verilated::commandArgs(argc, argv);
	string tests[4] = { "add", "sub", "Orange", "Yellow" };
	
	//vector<string> tests;
	ifstream infile("tests.txt");
	
	string test;

	while (getline(infile, test)) {
		//Instantiate our design
		VcpuTestbench *tb = new VcpuTestbench;

		Verilated::traceEverOn(true);
		VerilatedVcdC* tfp = new VerilatedVcdC;
		tb->trace(tfp, 99);
		tfp->open(("waveforms/wavefrom_" + test + ".vcd").c_str());

		// taking the program with the header removed
		ifstream input(("target_texts/" + test + ".text"), ios::binary);

		vector<unsigned char> buffer(istreambuf_iterator<char>(input), {});
		
		printf("Running test for %s: ", test.c_str());

		tb -> giveCtrlToCpu = 0;
		tb -> programmer_byteValid = 0;
		tb->reset = 1;
		tick(++tickcount, tb, tfp);
		tb->reset = 0;
		tick(++tickcount, tb, tfp);

		// programming the cpu
		tb -> programmer_byteValid = 1;
		for (int i = 0; i < buffer.size(); i++) {
			tb -> programmer_byte = buffer.at(i);
			tick(++tickcount, tb, tfp);
		}
		tb -> programmer_byteValid = 0;
		tb -> giveCtrlToCpu = 1;

		for (int i = 0; i < 4000; i++) {
			tick(++tickcount, tb, tfp);
			if (tb -> storeOut_value != 0) {
				break;
			}
			if(i == 3999) {
				printf("test faild: timeout\n");
			}
		}
		if (tb -> storeOut_value != 0){
			if (tb -> storeOut_value == 1) {
				printf("Test passed!\n");
			} else {
				printf("Test faild on a0 value :%d\n", (tb -> storeOut_value) >> 1);
			}
		}
	}
}
