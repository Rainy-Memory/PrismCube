package IR.Instruction;

import FrontEnd.IRVisitor;
import IR.IRBasicBlock;
import IR.Operand.IROperand;
import IR.Operand.IRRegister;
import IR.TypeSystem.IRPointerType;
import IR.TypeSystem.IRTypeSystem;
import MiddleEnd.Utils.CloneManager;

import java.util.Objects;
import java.util.function.Consumer;

public class IRLoadInstruction extends IRInstruction {
    private final IRTypeSystem loadType;
    private final IRRegister resultRegister;
    private IROperand loadAddress;

    public IRLoadInstruction(IRBasicBlock parentBlock, IRTypeSystem loadType, IRRegister resultRegister, IROperand loadAddress) {
        super(parentBlock);
        assert loadAddress.getIRType() instanceof IRPointerType;
        assert Objects.equals(loadType, ((IRPointerType) loadAddress.getIRType()).getBaseType());
        assert Objects.equals(loadType, resultRegister.getIRType());
        this.loadType = loadType;
        this.resultRegister = resultRegister;
        this.loadAddress = loadAddress;
        loadAddress.addUser(this);
        resultRegister.setDef(this);
    }

    public IRRegister getResultRegister() {
        return resultRegister;
    }

    public IROperand getLoadAddress() {
        return loadAddress;
    }

    public IRTypeSystem getLoadType() {
        return loadType;
    }

    @Override
    public void replaceUse(IROperand oldOperand, IROperand newOperand) {
        super.replaceUse(oldOperand, newOperand);
        if (loadAddress == oldOperand) {
            oldOperand.removeUser(this);
            loadAddress = newOperand;
            newOperand.addUser(this);
        }
    }

    @Override
    public void forEachNonLabelOperand(Consumer<IROperand> consumer) {
        consumer.accept(resultRegister);
        consumer.accept(loadAddress);
    }

    @Override
    public IRInstruction cloneMySelf(CloneManager m) {
        return new IRLoadInstruction(m.get(getParentBlock()), loadType, (IRRegister) m.get(resultRegister), m.get(loadAddress));
    }

    @Override
    public IRRegister getDef() {
        return resultRegister;
    }

    @Override
    public boolean noUsersAndSafeToRemove() {
        return resultRegister.getUsers().isEmpty();
    }

    @Override
    public String toString() {
        return resultRegister + " = load " + loadType + ", " + loadAddress.getIRType() + " " + loadAddress + (IRInstruction.useAlign() ? ", align " + loadType.sizeof() : "");
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
