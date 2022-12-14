package common

object common {
    val opcodes = Map(
        "lui"   -> "b????????????????????0110111",
        "auipc" -> "b????????????????????0010111",
        "jal"   -> "b????????????????????1101111",
        "jalr"  -> "b????????????000?????1100111",
        "beq"   -> "b????????????000?????1100011",
        "bne"   -> "b????????????001?????1100011",
        "blt"   -> "b????????????100?????1100011",
        "bge"   -> "b????????????101?????1100011",
        "bltu"  -> "b????????????110?????1100011",
        "bgeu"  -> "b????????????111?????1100011",
        "lb"    -> "b????????????000?????0000011",
        "lh"    -> "b????????????001?????0000011",
        "lw"    -> "b????????????010?????0000011",
        "lbu"   -> "b????????????100?????0000011",
        "lhu"   -> "b????????????000?????0000011",
        "sb"    -> "b????????????000?????0100011",
        "sh"    -> "b????????????001?????0100011",
        "sw"    -> "b????????????010?????0100011",
        "addi"  -> "b????????????000?????0010011",
        "addi"  -> "b????????????000?????0010011")

    def getreadInstr(instr: String = "ld x0, 8(x2)") = 
        "b????????????001?????0000011".replace('?', '0')
}