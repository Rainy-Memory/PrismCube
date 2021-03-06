package AST.PrimaryNode;

import AST.ASTVisitor;
import Utility.Cursor;

public class NumericalConstantPrimaryNode extends PrimaryNode {
    private final int numericalConstant;

    public NumericalConstantPrimaryNode(String numericalConstantText, Cursor cursor) {
        super(false, cursor);
        this.numericalConstant = Integer.parseInt(numericalConstantText);
    }

    public int getNumericalConstant() {
        return numericalConstant;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
