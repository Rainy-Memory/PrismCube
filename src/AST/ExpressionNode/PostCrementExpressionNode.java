package AST.ExpressionNode;

import AST.ASTVisitor;
import Utility.Cursor;

public class PostCrementExpressionNode extends ExpressionNode {
    private final ExpressionNode lhs;
    private final String op;

    public PostCrementExpressionNode(ExpressionNode lhs, String op, Cursor cursor) {
        super(false, cursor);
        this.lhs = lhs;
        this.op = op;
    }

    public ExpressionNode getLhs() {
        return lhs;
    }

    public String getOp() {
        return op;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
