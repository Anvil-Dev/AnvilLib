package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

/** A machine attached to a large cauldron's vapor outlet. */
@FunctionalInterface
public interface IVaporConsumer {
    /**
     * Returns the amount accepted from {@code vapor}. Implementations must not mutate state during simulation. When
     * execution immediately follows on the server thread, it receives a stack sized to the simulated accepted amount
     * and the consumer must accept that entire stack.
     */
    int receiveVapor(VaporStack vapor, VaporAction action, VaporizationContext context);

    /**
     * Whether this consumer seals the cauldron outlet and therefore applies backpressure when it cannot accept all
     * offered vapor. Open consumers return {@code false}: vapor they cannot accept escapes into the atmosphere.
     * This query must not mutate world or consumer state.
     */
    default boolean sealsOutlet(VaporizationContext context) {
        return false;
    }
}
