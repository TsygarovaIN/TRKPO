package nio;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class OperandsParserTest {
    private final double delta = 0.00001;

    private final Operand op1 = new Operand(OperandType.EMPTY, 1, OperandType.PLUS);
    private final Operand op2 = new Operand(OperandType.EMPTY, 1, OperandType.EQUALS);

    private final Operand op3 = new Operand(OperandType.COS, 0, OperandType.MINUS);
    private final Operand op4 = new Operand(OperandType.SIN, 0, OperandType.EQUALS);

    private final Operand op5 = new Operand(OperandType.COS, 10, OperandType.PLUS);
    private final Operand op6 = new Operand(OperandType.EMPTY, 5, OperandType.MINUS);
    private final Operand op7 = new Operand(OperandType.SQUARE, 100, OperandType.PLUS);
    private final Operand op8 = new Operand(OperandType.EMPTY, 7, OperandType.MULT);
    private final Operand op9 = new Operand(OperandType.ABS, -3, OperandType.EQUALS);

    private final List<Operand> operands = listOf(op5, op6, op7, op8, op9);

    private final Operand op10 = new Operand(OperandType.EXP, 1, OperandType.EQUALS);
    private final Operand op11 = new Operand(OperandType.LN, 10, OperandType.EQUALS);
    private final Operand op12 = new Operand(OperandType.EMPTY, 0, OperandType.EMPTY);
    private final Operand op13 = new Operand(OperandType.TAN, 1, OperandType.EQUALS);

    private final Operand op14 = new Operand(OperandType.EMPTY, 1, OperandType.DIVIDE);
    private final Operand op15 = new Operand(OperandType.EMPTY, 0, OperandType.EQUALS);

    private final Operand op16 = new Operand(OperandType.EMPTY, 1, OperandType.MULT);
    private final Operand op17 = new Operand(OperandType.SIN, Math.PI, OperandType.EQUALS);
    private final Operand op18 = new Operand(OperandType.COS, Math.PI, OperandType.EQUALS);
    private final Operand op19 = new Operand(OperandType.ABS, Math.PI, OperandType.EQUALS);
    
    private static List<Operand> listOf(Operand... operands) {
        return new ArrayList<>(Arrays.asList(operands));
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(20);

    @Test
    public void parseAndCalculateSimple() {
       assertEquals(2, OperandsParser.parseAndCalculate(listOf(op1, op2)), delta);
    }

    @Test
    public void parseAndCalculateBinary() {
        assertEquals(1, OperandsParser.parseAndCalculate(listOf(op3, op4)), delta);
    }

    @Test
    public void parseAndCalculateNonEquals() {
        assertEquals(Double.NaN, OperandsParser.parseAndCalculate(listOf(op1, op3)), delta);
    }

    @Test
    public void parseAndCalculateNorm() {
        Assert.assertEquals(3.48278, OperandsParser.parseAndCalculate(operands), delta);
    }

    //Ira
    @Test
    public void parseAndCalculateExp() {
        assertEquals(2.71828, OperandsParser.parseAndCalculate(listOf(op10)), delta);
    }

    @Test
    public void parseAndCalculateLn() {
        assertEquals(2.302585, OperandsParser.parseAndCalculate(listOf(op11)), delta);
    }

    @Test
    public void parseAndCalculateEmpty() {
        assertEquals(Double.NaN, OperandsParser.parseAndCalculate(listOf(op12)), delta);
    }

    @Test
    public void parseAndCalculateABS() {
        assertEquals(3, OperandsParser.parseAndCalculate(listOf(op9)), delta);
    }

    @Test
    public void parseAndCalculateTAN() {
        assertEquals(1.55741, OperandsParser.parseAndCalculate(listOf(op13)), delta);
    }

    @Test
    public void parseAndCalculateDivide0() {
        assertEquals(Double.POSITIVE_INFINITY, OperandsParser.parseAndCalculate(listOf(op14, op15)), delta);
    }

    @Test
    public void parseAndCalculateMult0() {
        assertEquals(0, OperandsParser.parseAndCalculate(listOf(op16, op15)), delta);
    }

    @Test
    public void parseAndCalculateSinPi() {
        assertEquals(0, OperandsParser.parseAndCalculate(listOf(op17)), delta);
    }

    @Test
    public void parseAndCalculateCosPi() {
        assertEquals(-1, OperandsParser.parseAndCalculate(listOf(op18)), delta);
    }

    @Test
    public void parseAndCalculateABSPi() {
        assertEquals(Math.PI, OperandsParser.parseAndCalculate(listOf(op19)), delta);
    }

}