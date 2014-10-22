package org.rioproject.opstring;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Options for time based undeployment of an {@code OperationalString}.
 *
 * @author Dennis Reedy
 */
public class UndeployOption implements Serializable {
    private static final long serialVersionUID = 1l;
    public enum Type {
        /**
         * If at least one service in an {@code OperationalString} implements the
         * {@link org.rioproject.admin.ServiceActivityProvider} interface, this option can be used
         * to undeploy if inactivity is determined for the duration of time provided.
         */
        WHEN_IDLE,

        /**
         * Undeploy the {@code OperationalString} on a specific date. This option should be used with care, since
         * services could still be running when the {@code OperationalString} is to be undeployed.
         */
        ON_DATE
    }
    private final Long when;
    private final TimeUnit timeUnit;
    private final Type type;

    /**
     * Default amount of time to check for idleness.
     */
    public static final Long DEFAULT_IDLE = TimeUnit.SECONDS.toMillis(5);

    /**
     * Create an {@code UndeployOption} based on {@code TimeUnit.MILLISECONDS}.
     *
     * @param when The when in milliseconds.
     * @param type The {@code Type of {@code UndeployOption}}
     *
     * @throws IllegalArgumentException if the value is <= 0
     */
    public UndeployOption(final Long when, final Type type) {
        this(when, type, TimeUnit.MILLISECONDS);
    }

    /**
     * Create an {@code UndeployOption}
     *
     * @param when The when in milliseconds.
     * @param type The {@code Type} of {@code UndeployOption}
     * @param timeUnit The {@code TimeUnit}s the {@code when} value is in
     *
     * @throws IllegalArgumentException if the when is <= 0, type is {@code null} or {@code timeUnit} is {@code null}.
     */
    public UndeployOption(final Long when, final Type type, final TimeUnit timeUnit) {
        if(when<=0 || type==null || timeUnit==null)
            throw new IllegalArgumentException("parameters cannot be null");
        this.when = when;
        this.type = type;
        this.timeUnit = timeUnit;
    }


    /**
     * Get the {@code when} property.
     *
     * @return The value of {@code when}
     */
    public Long getWhen() {
        return when;
    }

    /**
     * Get the {@code TimeUnit}.
     *
     * @return The {@code TimeUnit}.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Get the {@code Type}
     *
     * @return the {@code Type}
     */
    public Type getType() {
        return type;
    }
}
