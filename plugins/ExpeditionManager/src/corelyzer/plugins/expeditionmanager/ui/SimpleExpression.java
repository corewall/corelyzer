package corelyzer.plugins.expeditionmanager.ui;

/**
 * A simple expression parser.
 * 
 * @author Josh Reed (jareed@iastate.edu)
 */
public final class SimpleExpression {
    private static final String EMPTY = "";

    /**
     * Determines whether the string represents a double or not.
     * 
     * @param num
     *            the string to check.
     * 
     * @return true if the string represents a double, false otherwise.
     */
    public static boolean isDouble(final String num) {
        if (num == null) {
            return false;
        }

        try {
            Double.parseDouble(num);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Determines whether the string represents an int or not.
     * 
     * @param num
     *            the string to check.
     * 
     * @return true if the string represents an int, false otherwise.
     */
    public static boolean isInteger(final String num) {
        if (num == null) {
            return false;
        }

        try {
            Integer.parseInt(num);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Determines whether the string represents a number or not.
     * 
     * @param num
     *            the string to check.
     * 
     * @return true if the string represents a number, false otherwise.
     */
    public static boolean isNumber(final String num) {
        return isDouble(num) || isInteger(num);
    }

    /**
     * Parse a simple expression.
     * 
     * @param text
     *            the expression text.
     * @return the SimpleExpression or null.
     */
    public static SimpleExpression parse(final String text) {
        try {
            return new SimpleExpression(text);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    private String operator = EMPTY;

    private String operand1 = EMPTY;

    private String operand2 = EMPTY;

    /**
     * Creates and parses a new SimpleExpression.
     * 
     * @param expression
     *            the expression to parse.
     * 
     * @throws IllegalArgumentException
     *             thrown if there is a problem parsing the expression.
     */
    private SimpleExpression(final String expression)
            throws IllegalArgumentException {
        boolean inOp1 = false;
        boolean inOp2 = false;

        // build a simple state machine
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if ((c == '<') || (c == '>')) {
                if (operator.equals(EMPTY)) {
                    // we got our operator
                    operator += c;
                    inOp1 = true;
                } else {
                    // operator already set!
                    throw new IllegalArgumentException("Encountered a '" + c
                            + "' but operator was already set to '" + operator
                            + "'");
                }
            } else if (c == '-') {
                if (!inOp1 && !inOp2) {
                    inOp1 = true;
                    operand1 += c;
                } else if (inOp1) {
                    if (operator.equals(EMPTY)) {
                        operator += c;
                        inOp1 = false;
                        inOp2 = true;
                    } else {
                        operand1 += c;
                    }
                } else if (inOp2) {
                    operand2 += c;
                } else {
                    throw new IllegalArgumentException("Dunno what happened!");
                }
            } else {
                if (!inOp1 && !inOp2) {
                    operand1 += c;
                    inOp1 = true;
                } else if (inOp1) {
                    operand1 += c;
                } else if (inOp2) {
                    operand2 += c;
                }
            }
        }

        // successfully parsed
        operand1 = operand1.trim();
        operator = operator.trim();
        operand2 = operand2.trim();
    }

    /**
     * @return Returns the operand1.
     */
    public String getOperand1() {
        return operand1;
    }

    /**
     * @return Returns the operand2.
     */
    public String getOperand2() {
        return operand2;
    }

    /**
     * @return Returns the operator.
     */
    public String getOperator() {
        return operator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "<" + operand1 + "> " + operator + " <" + operand2 + ">";
    }
}
