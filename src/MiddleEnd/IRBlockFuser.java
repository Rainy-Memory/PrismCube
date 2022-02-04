package MiddleEnd;

import IR.IRBasicBlock;
import IR.IRFunction;
import IR.Instruction.IRJumpInstruction;
import Memory.Memory;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class IRBlockFuser {
    private boolean changed = true;

    public void fuse(Memory memory) {
        while (changed) memory.getIRModule().getFunctions().values().forEach(this::visit);
    }

    private void visit(IRFunction function) {
        changed = false;
        // remove unreachable blocks
        LinkedHashSet<IRBasicBlock> reachable = function.reachableBlocks();
        function.getBlocks().removeIf(block -> !reachable.contains(block));
        // cannot fuse entry block and exit block
        if (function.getBlocks().size() <= 2) return;
        // fuse blocks
        ArrayList<IRBasicBlock> blocks = new ArrayList<>(function.getBlocks());
        LinkedHashSet<IRBasicBlock> deleted = new LinkedHashSet<>();
        blocks.forEach(block -> {
            if (!deleted.contains(block) && block.getEscapeInstruction() instanceof IRJumpInstruction) {
                assert block.getSuccessors().size() == 1;
                IRBasicBlock succ = block.getSuccessors().get(0);
                if (succ.getPredecessors().size() == 1) {
                    assert succ.getPredecessors().get(0) == block;
                    block.fuse(succ);
                    function.getBlocks().remove(succ);
                    deleted.add(succ);
                    changed = true;
                }
            }
        });
    }
}
