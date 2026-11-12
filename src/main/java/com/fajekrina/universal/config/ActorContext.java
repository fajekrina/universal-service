package com.fajekrina.universal.config;

import com.fajekrina.universal.utils.constants.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;

/**
 * Who is acting in the current thread (request or startup job). Filled by {@link ActorFilter}
 * from the {@code X-SDUI-Actor} header; read by JPA auditing (created_by / updated_by) and the audit log.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActorContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static String current() {
        String actor = CURRENT.get();
        return actor == null || actor.isBlank() ? Constants.SYSTEM_ACTOR : actor;
    }

    static void set(String actor) {
        CURRENT.set(actor);
    }

    static void clear() {
        CURRENT.remove();
    }

    /** Run a block as {@code actor} (startup seeding, tests). */
    public static <T> T runAs(String actor, Supplier<T> block) {
        String previous = CURRENT.get();
        CURRENT.set(actor);
        try {
            return block.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
