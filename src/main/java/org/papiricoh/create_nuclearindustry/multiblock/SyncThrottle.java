package org.papiricoh.create_nuclearindustry.multiblock;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Decides when a block entity's client update is worth sending. A controller used to send its whole
 * state every ten ticks whether anything moved or not; now it describes what its clients show as a
 * row of numbers and sends only once one of them moved by a step a player could see, or when
 * {@code forceTicks} have passed since the last update, so a slow drift still arrives.
 *
 * <p>Value {@code i} counts as changed when it moved by at least {@code steps[i]} and by at least
 * {@code relative[i]} of its size. Both 0 means any change counts: flags, phases and counts. NaN
 * against a number is a change. Pure logic, no game classes, so it can be tested on its own.
 */
public final class SyncThrottle {
    /**
     * A record class's component accessors, looked up once: reading them by reflection on every check
     * (a fresh component array, {@code Method.invoke}) cost more than the comparison itself.
     */
    private static final ClassValue<MethodHandle[]> ACCESSORS = new ClassValue<>() {
        @Override
        protected MethodHandle[] computeValue(Class<?> type) {
            RecordComponent[] components = type.getRecordComponents();
            MethodHandle[] handles = new MethodHandle[components.length];
            for (int i = 0; i < components.length; i++) {
                try {
                    components[i].getAccessor().setAccessible(true);
                    handles[i] = MethodHandles.lookup().unreflect(components[i].getAccessor())
                            .asType(MethodType.methodType(Object.class, Object.class));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    throw new IllegalStateException("cannot read " + type.getName() + "." + components[i].getName(), e);
                }
            }
            return handles;
        }
    };

    private final double[] steps;
    private final double[] relative;
    /** Values at the last update sent; null before the first. */
    private double[] sent;
    private long sentAt;
    private boolean pending = true;
    private int sends;
    /** The row {@link #due(long, Record, int)} read last, reused for the next read and by {@link #sent(long, Supplier)}. */
    private double[] row;
    private long rowAt;
    private boolean rowFresh;

    public SyncThrottle(double[] steps, double[] relative) {
        if (steps.length != relative.length) {
            throw new IllegalArgumentException("one step and one relative step per value");
        }
        this.steps = steps.clone();
        this.relative = relative.clone();
    }

    /**
     * A throttle for the components of a record, in declaration order (see {@link #valuesOf}):
     * booleans, enums and whole numbers count on any change, decimals once they moved by
     * {@code relativeStep} of their size and by {@code minimumStep}. {@code fixedSteps} gives a
     * component, by name, a fixed step instead (a tank's 1%, say).
     */
    public static SyncThrottle forRecord(Class<? extends Record> type, double relativeStep, double minimumStep,
                                         Map<String, Double> fixedSteps) {
        RecordComponent[] components = type.getRecordComponents();
        double[] steps = new double[components.length];
        double[] relative = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            Class<?> kind = components[i].getType();
            Double fixed = fixedSteps.get(components[i].getName());
            if (fixed != null) {
                steps[i] = fixed;
            } else if (kind == double.class || kind == float.class) {
                steps[i] = minimumStep;
                relative[i] = relativeStep;
            }
        }
        return new SyncThrottle(steps, relative);
    }

    /** A record's components as numbers: booleans 0/1, enums by ordinal, numbers as they are, anything else by hash. */
    public static double[] valuesOf(Record record) {
        return valuesOf(record, null);
    }

    /** {@link #valuesOf(Record)} into {@code into} when it has the right length, else into a new array. */
    public static double[] valuesOf(Record record, double[] into) {
        MethodHandle[] accessors = ACCESSORS.get(record.getClass());
        double[] values = into != null && into.length == accessors.length ? into : new double[accessors.length];
        for (int i = 0; i < accessors.length; i++) {
            Object value;
            try {
                value = accessors[i].invoke((Object) record);
            } catch (Throwable e) {
                throw new IllegalStateException("cannot read component " + i + " of " + record.getClass().getName(), e);
            }
            values[i] = switch (value) {
                case null -> 0.0;
                case Boolean flag -> flag ? 1.0 : 0.0;
                case Enum<?> constant -> constant.ordinal();
                case Number number -> number.doubleValue();
                default -> value.hashCode();
            };
        }
        return values;
    }

    /** Whether an update is due now: never sent, invalidated, a value moved a visible step, or too long since the last. */
    public boolean due(long now, double[] values, int forceTicks) {
        if (pending || sent == null || sent.length != values.length || now - sentAt >= forceTicks || now < sentAt) {
            return true;
        }
        for (int i = 0; i < values.length; i++) {
            if (visiblyDifferent(sent[i], values[i], i < steps.length ? steps[i] : 0.0, i < relative.length ? relative[i] : 0.0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@link #due(long, double[], int)} for a record's components, read into a row this throttle keeps
     * and reuses; {@link #sent(long, Supplier)} later in the same tick takes that row instead of reading again.
     */
    public boolean due(long now, Record readings, int forceTicks) {
        row = valuesOf(readings, row);
        rowAt = now;
        rowFresh = true;
        return due(now, row, forceTicks);
    }

    /**
     * Records an update sent at {@code now} showing {@code readings}: the row {@link #due(long, Record, int)}
     * read this tick if there is one (the update follows the check), otherwise the record made now.
     */
    public void sent(long now, Supplier<? extends Record> readings) {
        if (!rowFresh || rowAt != now) {
            row = valuesOf(readings.get(), row);
        }
        rowFresh = false;
        sent(now, row);
    }

    /** Records an update sent at {@code now} showing {@code values}. */
    public void sent(long now, double[] values) {
        if (sent != null && sent.length == values.length) {
            System.arraycopy(values, 0, sent, 0, values.length);
        } else {
            sent = values.clone();
        }
        sentAt = now;
        pending = false;
        sends++;
    }

    /** The next {@link #due} answers yes, whatever the values: something the numbers do not cover changed. */
    public void invalidate() {
        pending = true;
    }

    /**
     * For block entities whose update packets carry only what changed since an earlier broadcast:
     * whether a broadcast at {@code now} goes out in a packet of its own, after the last broadcast's.
     * Not when no update packet was made since that one (it has not gone out yet, or nobody watches),
     * nor in the same game tick (both go out together); then the next packet must still carry what the
     * last broadcast's would have.
     */
    public static boolean startsNewBroadcast(boolean packetSinceBroadcast, long broadcastTime, long now) {
        return packetSinceBroadcast && now != broadcastTime;
    }

    /** Updates recorded with {@link #sent}, for tests. */
    public int sends() {
        return sends;
    }

    /**
     * Whether {@code a} and {@code b} would look different: they differ by at least {@code step} and
     * by at least {@code relative} of the larger of the two.
     */
    public static boolean visiblyDifferent(double a, double b, double step, double relative) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) != Double.isNaN(b);
        }
        if (a == b) {
            return false;
        }
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return true;
        }
        double difference = Math.abs(a - b);
        return difference >= step && difference >= relative * Math.max(Math.abs(a), Math.abs(b));
    }
}
