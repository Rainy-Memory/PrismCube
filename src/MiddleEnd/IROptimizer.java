package MiddleEnd;

import FrontEnd.IREmitter;
import IR.Instruction.IRInstruction;
import Memory.Memory;
import MiddleEnd.IROptimize.*;

import java.util.concurrent.atomic.AtomicInteger;

import static Debug.MemoLog.log;

public class IROptimizer extends Optimize {
    static private final int rounds = 30;

    private void ssaOptimize(Memory memory, String msg) {
        int cnt = 0;
        boolean changed;
        while (cnt++ < rounds) {
            log.Infof("IR Optimize %sround %d\n", msg, cnt);
            changed = false;

            new IREmitter().emitDebug(memory, String.format("./bin/opt-%s%s-before-%d.ll", msg, "adce", cnt));
            changed |= new AggressiveDeadCodeEliminator().eliminate(memory);
            new ControlFlowGraphChecker("after adce in round " + cnt).check(memory);

            new IREmitter().emitDebug(memory, String.format("./bin/opt-%s%s-before-%d.ll", msg, "sccp", cnt));
            changed |= new SparseConditionalConstantPropagator().propagate(memory);
            new ControlFlowGraphChecker("after sccp in round " + cnt).check(memory);

            new IREmitter().emitDebug(memory, String.format("./bin/opt-%s%s-before-%d.ll", msg, "loop", cnt));
            changed |= new LoopOptimizer().doi(memory);
            new ControlFlowGraphChecker("after loop in round " + cnt).check(memory);

            new IREmitter().emitDebug(memory, String.format("./bin/opt-%s%s-before-%d.ll", msg, "inline", cnt));
            changed |= new FunctionInliner().inline(memory);
            new ControlFlowGraphChecker("after inline in round " + cnt).check(memory);

            new IREmitter().emitDebug(memory, String.format("./bin/opt-%s%s-before-%d.ll", msg, "fuse", cnt));
            changed |= new IRBlockFuser().fuse(memory);
            new ControlFlowGraphChecker("after fuse in round " + cnt).check(memory);

            new IREmitter().emitDebug(memory, String.format("./bin/opt-%soptimize-after-%d.ll", msg, cnt));
            if (!changed) {
                log.Infof("No changed has made in this turn.\n");
                break;
            }
        }
    }

    public void invoke(Memory memory) {
        if (level == OptimizeLevel.O0) return;

        new IREmitter().emitDebug(memory, "./bin/opt-mem2reg-before.ll");
        new MemoryToRegisterPromoter().promote(memory);

        if (level != OptimizeLevel.O1) {
            FunctionInliner.disableForceInline();
            ssaOptimize(memory, "");

            FunctionInliner.enableForceInline();
            ssaOptimize(memory, "force-inline-");
        }

        new IREmitter().emitOpt(memory);
        new PhiResolver().resolve(memory);

        IRInstruction.checkRemoved();
        new IREmitter().emitDebug(memory, "./bin/opt-phiResolve-after.ll");

        AtomicInteger num = new AtomicInteger();
        memory.getIRModule().getFunctions().values().forEach(function -> {
            log.Tracef("block size (%s): %d\n", function, function.getBlocks().size());
            num.addAndGet(function.getBlocks().size());
        });
        log.Tracef("block size (in total): %d\n", num.get());
    }
}
