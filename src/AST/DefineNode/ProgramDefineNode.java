package AST.DefineNode;

import AST.StatementNode.StatementNode;
import Utility.Cursor;

public abstract class ProgramDefineNode extends StatementNode {
    public ProgramDefineNode(Cursor cursor) {
        super(cursor);
    }
}
