#! /bin/sh
cd ..
sbt "runMain pipeline.alu.axi_cpu_wrapper"

cd cpu-test

mv ../axi_cpu_wrapper.v .

echo '/* verilator lint_off UNUSED */' | cat - axi_cpu_wrapper.v > temp && mv temp axi_cpu_wrapper.v
echo '/* verilator lint_off DECLFILENAME */' | cat - axi_cpu_wrapper.v > temp && mv temp axi_cpu_wrapper.v


verilator -Wall --trace -cc cpuTestbench.v

cd obj_dir

make -f VcpuTestbench.mk

cd ..

g++ -I /usr/share/verilator/include -I obj_dir /usr/share/verilator/include/verilated.cpp /usr/share/verilator/include/verilated_vcd_c.cpp cpuTestbench.cpp obj_dir/VcpuTestbench__ALL.a -o cpuTestbench

./cpuTestbench
