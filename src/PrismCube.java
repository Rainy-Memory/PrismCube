import FrontEnd.*;
import BackEnd.*;
import Memory.Memory;
import MiddleEnd.DominatorTreeBuilder;
import MiddleEnd.IRBlockFuser;
import MiddleEnd.IRGlobalInitializeEliminator;
import MiddleEnd.MemoryToRegisterPromoter;
import Utility.error.error;

/**
 * This class compiles source code to rv32i asm.
 *
 * @author rainy memory
 * @version 1.0.0
 */

public class PrismCube {
    public static void main(String[] args) throws Exception {
        try {
            // command line argument
            Memory memory = new Memory().parse(args);
            // abstract syntax tree
            new Preprocessor().preprocess(memory);
            new ASTBuilder().build(memory);
            // semantic
            new SymbolCollector().collect(memory);
            new SemanticChecker().check(memory);
            // intermediate representation
            new ConstStringCollector().collect(memory);
            new IRBuilder().build(memory);
            new IREmitter().emit(memory);
            // optimize
            new IRGlobalInitializeEliminator().eliminate(memory);
            new IRBlockFuser().fuse(memory);
            new DominatorTreeBuilder().build(memory);
            new MemoryToRegisterPromoter().promote(memory);
            new IREmitter().emitOpt(memory);
            // assembly
            new InstructionSelector().select(memory);
            new RegisterAllocator().allocate(memory);
            new ASMEmitter().emit(memory);
        } catch (error err) {
            err.printStackTrace();
            throw new RuntimeException();
        }
    }
}
