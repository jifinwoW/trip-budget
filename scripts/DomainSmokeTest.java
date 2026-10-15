import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone smoke checks mirroring domain formulas (no Android SDK required).
 * Run: javac scripts/DomainSmokeTest.java && java -cp scripts DomainSmokeTest
 */
public class DomainSmokeTest {
    static int failures = 0;

    public static void main(String[] args) {
        testPartialPayments();
        testExcess();
        testSharedVsPersonal();
        testOverspend();
        testZeroBudget();
        testOutstandingPositiveOnly();
        testMoneyParse();
        if (failures > 0) {
            System.err.println(failures + " failure(s)");
            System.exit(1);
        }
        System.out.println("All domain smoke checks passed.");
    }

    static void testPartialPayments() {
        Totals t = calc(1_000_000L, map(1L, 500_000L, 2L, 500_000L), map(1L, 200_000L, 2L, 500_000L), List.of());
        check("collected", 700_000L, t.collected);
        check("outstanding sum", 300_000L, t.outstandingSum);
    }

    static void testExcess() {
        Totals t = calc(500_000L, map(1L, 100_000L), map(1L, 150_000L), List.of());
        check("excess", 50_000L, t.excessFor(1L));
        check("outstanding sum with excess", 0L, t.outstandingSum);
    }

    static void testSharedVsPersonal() {
        List<Expense> expenses = List.of(
                new Expense(200_000L, true),
                new Expense(150_000L, false)
        );
        Totals t = calc(1_000_000L, map(1L, 500_000L), map(1L, 500_000L), expenses);
        check("total expenses", 350_000L, t.totalExpenses);
        check("shared cash", 300_000L, t.sharedCash);
        check("budget remaining", 650_000L, t.budgetRemaining);
    }

    static void testOverspend() {
        Totals t = calc(100_000L, Map.of(), Map.of(), List.of(new Expense(150_000L, true)));
        check("overspend remaining", -50_000L, t.budgetRemaining);
        check("is over", true, t.budgetRemaining < 0);
    }

    static void testZeroBudget() {
        Totals t = calc(0L, map(1L, 0L), map(1L, 10_000L), List.of(new Expense(5_000L, true)));
        check("zero budget remaining", -5_000L, t.budgetRemaining);
        check("zero budget shared cash", 5_000L, t.sharedCash);
    }

    static void testOutstandingPositiveOnly() {
        Totals t = calc(1_000_000L, map(1L, 100_000L, 2L, 100_000L), map(1L, 50_000L, 2L, 200_000L), List.of());
        check("positive outstanding only", 50_000L, t.outstandingSum);
    }

    static void testMoneyParse() {
        check("100 rupees", 10_000L, rupeesToPaise("100"));
        check("100.55", 10_055L, rupeesToPaise("100.55"));
        check("invalid", null, rupeesToPaise("abc"));
    }

    static Long rupeesToPaise(String rupees) {
        try {
            BigDecimal value = new BigDecimal(rupees.trim());
            if (value.compareTo(BigDecimal.ZERO) < 0) return null;
            return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return null;
        }
    }

    static Map<Long, Long> map(Long k1, Long v1) {
        Map<Long, Long> m = new HashMap<>();
        m.put(k1, v1);
        return m;
    }

    static Map<Long, Long> map(Long k1, Long v1, Long k2, Long v2) {
        Map<Long, Long> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    static Totals calc(
            long budget,
            Map<Long, Long> expected,
            Map<Long, Long> paid,
            List<Expense> expenses
    ) {
        long collected = paid.values().stream().mapToLong(Long::longValue).sum();
        long totalExpenses = expenses.stream().mapToLong(e -> e.amount).sum();
        long shared = expenses.stream().filter(e -> e.shared).mapToLong(e -> e.amount).sum();
        long outstandingSum = 0;
        Map<Long, Long> excess = new HashMap<>();
        for (Map.Entry<Long, Long> e : expected.entrySet()) {
            long outstanding = e.getValue() - paid.getOrDefault(e.getKey(), 0L);
            if (outstanding > 0) outstandingSum += outstanding;
            if (outstanding < 0) excess.put(e.getKey(), -outstanding);
        }
        return new Totals(collected, totalExpenses, collected - shared, budget - totalExpenses, outstandingSum, excess);
    }

    static void check(String name, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.err.println("FAIL " + name + ": expected " + expected + " got " + actual);
        } else {
            System.out.println("OK   " + name);
        }
    }

    record Expense(long amount, boolean shared) {}

    record Totals(
            long collected,
            long totalExpenses,
            long sharedCash,
            long budgetRemaining,
            long outstandingSum,
            Map<Long, Long> excess
    ) {
        long excessFor(long memberId) {
            return excess.getOrDefault(memberId, 0L);
        }
    }
}
