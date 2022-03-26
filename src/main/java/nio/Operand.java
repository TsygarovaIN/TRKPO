package nio;

import java.io.Serializable;


public class Operand implements Serializable {
    private final OperandType operationFirst;
    private final double a;
    private final OperandType operationSecond;

    public Operand(double a, OperandType operationSecond) {
        this(OperandType.EMPTY, a, operationSecond);
    }

    public Operand(OperandType operationFirst, double a, OperandType operationSecond) {
        this.operationFirst = operationFirst;
        this.a = a;
        this.operationSecond = operationSecond;
    }

    public OperandType getOperationFirst() {
        return operationFirst;
    }

    public double getA() {
        return a;
    }

    public OperandType getOperationSecond() {
        return operationSecond;
    }

    @Override
    public String toString() {
        return operationFirst + " " + a + " " + operationSecond + " ";
    }
}
