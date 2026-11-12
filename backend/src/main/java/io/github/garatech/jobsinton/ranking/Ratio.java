package io.github.garatech.jobsinton.ranking;

/** Reduced exact fraction. Products of two int operands fit in a long. */
public record Ratio(int numerator, int denominator) implements Comparable<Ratio> {
    public Ratio {
        if (denominator <= 0) throw new IllegalArgumentException("Denominator must be positive.");
        long a = Math.abs((long) numerator), b = denominator;
        while (b != 0) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        numerator /= (int) a;
        denominator /= (int) a;
    }

    @Override
    public int compareTo(Ratio other) {
        return Long.compare((long) numerator * other.denominator, (long) other.numerator * denominator);
    }
}
