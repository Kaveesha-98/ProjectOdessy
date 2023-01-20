#! /bin/sh
cd ../..
sbt "runMain pipeline.decode.decodeHWTestbench"

cd verilator-tests/decodeHWTestbench

mv ../../decodeHWTestbench.v .

echo '/* verilator lint_off UNUSED */' | cat - decodeHWTestbench.v > temp && mv temp decodeHWTestbench.v
echo '/* verilator lint_off DECLFILENAME */' | cat - decodeHWTestbench.v > temp && mv temp decodeHWTestbench.v


verilator -Wall --trace -cc decodeHWTestbench.v

cd obj_dir

make -f VdecodeHWTestbench.mk

cd ..

g++ -I /usr/share/verilator/include -I obj_dir /usr/share/verilator/include/verilated.cpp /usr/share/verilator/include/verilated_vcd_c.cpp decodeHWTestbenchtest.cpp obj_dir/VdecodeHWTestbench__ALL.a -o decodeHWTestbench.out
