package MiddleEnd;

import IR.IRBasicBlock;
import IR.IRFunction;
import IR.Instruction.IRJumpInstruction;
import Memory.Memory;
import MiddleEnd.Pass.IRFunctionPass;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import static Debug.MemoLog.log;

public class IRBlockFuser implements IRFunctionPass {
    private boolean changed = false;

    public boolean fuse(Memory memory) {
        memory.getIRModule().getFunctions().values().forEach(this::visit);
        memory.getIRModule().getFunctions().values().forEach(this::visit);
        memory.getIRModule().removeUnusedFunction();
        if (changed) log.Infof("Program changed in fuse.\n");
        return changed;
    }

    @Override
    public void visit(IRFunction function) {
        // remove unreachable blocks
        changed |= function.removeUnreachableBlocks();
        // cannot fuse entry block and exit block
        if (function.getBlocks().size() <= 2) return;
        // fuse blocks
        ArrayList<IRBasicBlock> blocks = new ArrayList<>(function.getBlocks());
        LinkedHashSet<IRBasicBlock> deleted = new LinkedHashSet<>();
        blocks.forEach(block -> {
            if (!deleted.contains(block) && block.getEscapeInstruction() instanceof IRJumpInstruction) {
                assert block.getSuccessors().size() == 1 : "jump: [" + block.getEscapeInstruction() + "], succ: [" + block.getSuccessors() + "]";
                IRBasicBlock succ = block.getSuccessors().get(0);
                if (succ.getPredecessors().size() == 1) {
                    assert succ.getPredecessors().get(0) == block;
                    block.fuse(succ);
                    function.getBlocks().remove(succ);
                    // ensure return block is the last element of blocks
                    if (block.isReturnBlock()) function.relocateReturnBlock(block);
                    deleted.add(succ);
                    changed = true;
                }
            }
        });
    }
}
