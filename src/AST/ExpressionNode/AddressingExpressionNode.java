package AST.ExpressionNode;

import AST.ASTVisitor;
import Utility.Cursor;

public class AddressingExpressionNode extends LeftValueExpressionNode {
    private final ExpressionNode array;
    private final ExpressionNode index;

    public AddressingExpressionNode(ExpressionNode array, ExpressionNode index, Cursor cursor) {
        super(true, cursor);
        this.array = array;
        this.index = index;
    }

    public ExpressionNode getArray() {
        return array;
    }

    public ExpressionNode getIndex() {
        return index;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
