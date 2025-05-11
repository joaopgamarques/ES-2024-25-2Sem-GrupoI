package iscteiul.ista;

/**
 * Represents a potential swap between two properties (p1 and p2), along with
 * the associated benefit, cost, and overall score of performing that swap.
 *
 * <p>This is typically used to suggest an exchange of properties between
 * different owners, where the similarity in area (or other metrics) may
 * increase the likelihood of a mutually beneficial transaction.</p>
 */
public class SwapSuggestion {

    /**
     * The first property involved in the swap.
     */
    private final PropertyRecord p1;

    /**
     * The second property involved in the swap.
     */
    private final PropertyRecord p2;

    /**
     * A numeric indicator of the potential advantage or value gained by
     * performing the swap (e.g., how much this swap improves average area).
     */
    private final double benefit;

    /**
     * A numeric indicator of the likely cost or difficulty of performing
     * the swap (e.g., how different the property sizes are).
     */
    private final double cost;

    /**
     * An overall score that may combine multiple factors (e.g., a function
     * of benefit and cost) to provide a single ranking value for this swap.
     */
    private final double score;

    /**
     * Constructs a {@code SwapSuggestion} with all relevant metrics.
     *
     * @param p1      the first property in the swap
     * @param p2      the second property in the swap
     * @param benefit a numeric measure of the benefit gained from this swap
     * @param cost    a numeric measure of the cost or difficulty of this swap
     * @param score   an aggregated score for this swap, typically derived
     *                from benefit and cost
     */
    public SwapSuggestion(PropertyRecord p1, PropertyRecord p2,
                          double benefit, double cost, double score) {
        this.p1 = p1;
        this.p2 = p2;
        this.benefit = benefit;
        this.cost = cost;
        this.score = score;
    }

    /**
     * Returns the first property involved in the swap.
     *
     * @return the first {@link PropertyRecord}
     */
    public PropertyRecord getP1() {
        return p1;
    }

    /**
     * Returns the second property involved in the swap.
     *
     * @return the second {@link PropertyRecord}
     */
    public PropertyRecord getP2() {
        return p2;
    }

    /**
     * Returns the numeric indicator of the potential advantage of performing
     * this swap.
     *
     * @return the benefit of this swap
     */
    public double getBenefit() {
        return benefit;
    }

    /**
     * Returns the numeric indicator of the likely cost or difficulty of
     * performing this swap.
     *
     * @return the cost of this swap
     */
    public double getCost() {
        return cost;
    }

    /**
     * Returns the overall score for this swap, typically reflecting how
     * favorable or efficient the swap is when combining benefit and cost.
     *
     * @return the score of this swap
     */
    public double getScore() {
        return score;
    }

    /**
     * Provides a formatted string representation of this swap suggestion,
     * including both property IDs, their owners, and the computed metrics.
     *
     * @return a string describing this {@code SwapSuggestion}
     */
    @Override
    public String toString() {
        return String.format(
                "SwapSuggestion: [Prop %d (owner=%d), Prop %d (owner=%d)] => benefit=%.3f, cost=%.3f, score=%.3f",
                p1.getObjectID(), p1.getOwner(), p2.getObjectID(), p2.getOwner(),
                benefit, cost, score
        );
    }
}
