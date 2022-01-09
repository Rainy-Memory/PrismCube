package ASM.Operand;

public class ASMImmediate extends ASMOperand {
    int imm;

    public ASMImmediate(int imm) {
        this.imm = imm;
    }

    public void updateImmediate(int offset) {
        imm += offset;
    }

    public int getImm() {
        return imm;
    }

    @Override
    public String toString() {
        return Integer.toString(imm);
    }
}