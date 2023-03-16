#! /bin/sh
rm -r m_asm
mkdir m_asm && cd m_asm
for file in ../../../riscv-tests/isa/rv64um/*.S; do
  ~/riscv/bin/riscv64-unknown-elf-gcc -I ../../../riscv-tests/isa/macros/scalar/ -I ../../../riscv-tests/env/v/ -save-temps $file
done

for file in *; do
  sed -i -e 's/extra_boot; extra_boot: ret; .global userstart; userstart: init/_start\n\n_start:/g' $file
done

for file in *; do
  sed -i -e 's/bne x0, gp, pass; fail: sll a0, gp, 1; 1:beqz a0, 1b; or a0, a0, 1; scall;; pass: li a0, 1; scall/bne x0, gp, pass; fail: sll a0, gp, 1; 1:beqz a0, 1b; or a0, a0, 1; sb a0, -1(zero);; pass: li a0, 1; sb a0, -1(zero)/g' $file
done

rm -r *.o

cd ..
rm -r m_obj
mkdir m_obj && cd m_obj
for file in ../m_asm/*.s; do
  ~/riscv/bin/riscv64-unknown-elf-as $file -o `basename "$file" ".s"`.o
done

cd ..
rm -r m_elf
mkdir m_elf && cd m_elf
for file in ../m_obj/*.o; do
  ~/riscv/bin/riscv64-unknown-elf-ld -T ../target_assem/virt.ld "$file" -o `basename "$file" ".o"`
done

cd ..
rm -r m_dump
mkdir m_dump && cd m_dump
for file in ../m_elf/*; do
  ~/riscv/bin/riscv64-unknown-elf-objdump -S "$file" > `basename "$file"`.dump
done

cd ..
rm -r m_image
mkdir m_image && cd m_image
for file in ../m_elf/*; do
  ~/riscv/bin/riscv64-unknown-elf-objcopy -O binary "$file" `basename "$file"`.bin
done