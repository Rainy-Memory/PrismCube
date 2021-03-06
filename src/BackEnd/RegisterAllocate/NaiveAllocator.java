package BackEnd.RegisterAllocate;

import ASM.ASMBasicBlock;
import ASM.ASMFunction;
import ASM.Instruction.ASMArithmeticInstruction;
import ASM.Instruction.ASMInstruction;
import ASM.Instruction.ASMMemoryInstruction;
import ASM.Instruction.ASMPseudoInstruction;
import ASM.Operand.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static Debug.MemoLog.log;

/**
 * This class allocate virtual register to physical
 * register. In this naive implementation, all virtual
 * register are spilled to stack.
 *
 * @author rainy memory
 * @version 1.0.0
 */

public class NaiveAllocator {
    private static final ArrayList<ASMPhysicalRegister> registers = new ArrayList<>(Arrays.asList(
            ASMPhysicalRegister.getPhysicalRegister(ASMPhysicalRegister.PhysicalRegisterName.t0),
            ASMPhysicalRegister.getPhysicalRegister(ASMPhysicalRegister.PhysicalRegisterName.t1),
            ASMPhysicalRegister.getPhysicalRegister(ASMPhysicalRegister.PhysicalRegisterName.t2)
    ));

    private final ASMFunction function;
    private final LinkedHashMap<ASMVirtualRegister, ASMAddress> vr2addr = new LinkedHashMap<>();
    private ASMBasicBlock block;
    private ArrayList<ASMInstruction> newList;

    public NaiveAllocator(ASMFunction function) {
        this.function = function;
    }

    static private boolean isValidImmediate(int imm) {
        return -2048 <= imm && imm <= 2047;
    }

    static private ASMPhysicalRegister s(int i) {
        return ASMPhysicalRegister.getStoreRegister(i);
    }

    public void allocate() {
        // following code will store some value in s0 - s11, for we don't actually use physical registers except for t0, t1, t2
        // and these save registers are callee save and will be saved by all builtin function we called
        // simultaneously we need to back up these register as well
        ASMPhysicalRegister sp = ASMPhysicalRegister.getPhysicalRegister(ASMPhysicalRegister.PhysicalRegisterName.sp);
        ArrayList<ASMAddress> sBackup = new ArrayList<>();
        sBackup.add(null); // don't need to back up s0
        for (int i = 1; i <= 11; i++) sBackup.add(new ASMAddress(sp, new ASMImmediate(function.getStackFrame().spillToStack())));
        log.Debugf("start allocate register for function %s\n", function.getFunctionName());
        // calculate frame size
        function.getBlocks().forEach(block -> block.getInstructions().forEach(inst -> {
            if (inst != null) inst.getOperands().forEach(operand -> {
                if (operand instanceof ASMVirtualRegister && !vr2addr.containsKey(((ASMVirtualRegister) operand))) {
                    int offset = function.getStackFrame().spillToStack();
                    log.Debugf("request a word at %d" + " ".repeat(6 - Integer.toString(offset).length()) + "for virtual register %s\n", offset, ((ASMVirtualRegister) operand).getName());
                    ASMAddress address;
                    // use si to store sp + 2048 * i
                    if (isValidImmediate(offset)) address = new ASMAddress(sp, new ASMImmediate(offset));
                    else address = new ASMAddress(s(offset / 2048), new ASMImmediate(offset % 2048));
                    vr2addr.put((ASMVirtualRegister) operand, address);
                }
            });
        }));
        // minus & plus sp
        int frameSize = function.getStackFrame().getFrameSize();
        ASMBasicBlock entry = function.getBlocks().get(0), escape = function.getBlocks().get(function.getBlocks().size() - 1);
        int indexOfMinusSp = entry.getInstructions().indexOf(null), indexOfPlusSp = escape.getInstructions().indexOf(null);
        ASMArithmeticInstruction minusSp, plusSp;
        if (isValidImmediate(frameSize)) {
            minusSp = new ASMArithmeticInstruction(entry, ASMArithmeticInstruction.InstType.addi);
            minusSp.addOperand(sp).addOperand(sp).addOperand(new ASMImmediate(-frameSize));
            plusSp = new ASMArithmeticInstruction(escape, ASMArithmeticInstruction.InstType.addi);
            plusSp.addOperand(sp).addOperand(sp).addOperand(new ASMImmediate(frameSize));
        } else {
            ASMPseudoInstruction liNegative = new ASMPseudoInstruction(entry, ASMPseudoInstruction.InstType.li);
            liNegative.addOperand(s(0)).addOperand(new ASMImmediate(-frameSize));
            minusSp = new ASMArithmeticInstruction(entry, ASMArithmeticInstruction.InstType.add);
            minusSp.addOperand(sp).addOperand(sp).addOperand(s(0));
            entry.getInstructions().add(indexOfMinusSp++, liNegative);
            ASMPseudoInstruction liPositive = new ASMPseudoInstruction(escape, ASMPseudoInstruction.InstType.li);
            liPositive.addOperand(s(0)).addOperand(new ASMImmediate(frameSize));
            plusSp = new ASMArithmeticInstruction(escape, ASMArithmeticInstruction.InstType.add);
            plusSp.addOperand(sp).addOperand(sp).addOperand(s(0));
            escape.getInstructions().add(indexOfPlusSp++, liPositive);
            // si stores sp + 2048 * i
            int indexOfInitializeStore = indexOfMinusSp + 1;
            int indexOfRetrieveBackup = indexOfPlusSp - 1;
            for (int i = 1; i <= frameSize / 2048; i++) {
                // backup si to stack
                ASMMemoryInstruction backup = new ASMMemoryInstruction(entry, ASMMemoryInstruction.InstType.sw, s(i), sBackup.get(i));
                entry.getInstructions().add(indexOfInitializeStore++, backup);
                ASMMemoryInstruction retrieve = new ASMMemoryInstruction(escape, ASMMemoryInstruction.InstType.lw, s(i), sBackup.get(i));
                escape.getInstructions().add(indexOfRetrieveBackup++, retrieve);
                indexOfPlusSp++;
                // store sp + 2048 * i to si
                ASMPseudoInstruction li2si = new ASMPseudoInstruction(entry, ASMPseudoInstruction.InstType.li);
                li2si.addOperand(ASMPhysicalRegister.getStoreRegister(i)).addOperand(new ASMImmediate(i * 2048));
                entry.getInstructions().add(indexOfInitializeStore++, li2si);
                ASMArithmeticInstruction add2si = new ASMArithmeticInstruction(entry, ASMArithmeticInstruction.InstType.add);
                add2si.addOperand(ASMPhysicalRegister.getStoreRegister(i)).addOperand(sp).addOperand(ASMPhysicalRegister.getStoreRegister(i));
                entry.getInstructions().add(indexOfInitializeStore++, add2si);
            }
        }
        entry.getInstructions().set(indexOfMinusSp, minusSp);
        escape.getInstructions().set(indexOfPlusSp, plusSp);
        // step into block
        function.getBlocks().forEach(this::allocateBlock);
    }

    private void allocateBlock(ASMBasicBlock block) {
        newList = new ArrayList<>();
        this.block = block;
        block.getInstructions().forEach(this::allocateInstruction);
        block.setInstructions(newList);
    }

    private void allocateInstruction(ASMInstruction inst) {
        int current = 0;
        ArrayList<ASMRegister> use = new ArrayList<>(inst.getUses()), def = new ArrayList<>(inst.getDefs());
        for (ASMRegister rs : use) {
            if (rs instanceof ASMVirtualRegister) {
                ASMPhysicalRegister phyReg = registers.get(current++);
                newList.add(new ASMMemoryInstruction(block, ASMMemoryInstruction.InstType.lw, phyReg, vr2addr.get((ASMVirtualRegister) rs)));
                inst.replaceRegister((ASMVirtualRegister) rs, phyReg);
            }
        }
        newList.add(inst);
        for (ASMRegister rd : def) {
            if (rd instanceof ASMVirtualRegister) {
                ASMPhysicalRegister phyReg = registers.get(current++);
                newList.add(new ASMMemoryInstruction(block, ASMMemoryInstruction.InstType.sw, phyReg, vr2addr.get((ASMVirtualRegister) rd)));
                inst.replaceRegister((ASMVirtualRegister) rd, phyReg);
            }
        }
    }
}
