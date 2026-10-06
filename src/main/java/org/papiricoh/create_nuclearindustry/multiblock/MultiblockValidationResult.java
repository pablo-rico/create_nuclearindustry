package org.papiricoh.create_nuclearindustry.multiblock;

import java.util.List;
import java.util.Optional;

/** Outcome of validating a multiblock: the structure when formed, plus errors and warnings. */
public record MultiblockValidationResult<S>(boolean formed, Optional<S> structure, List<String> errors, List<String> warnings) {

    public static <S> MultiblockValidationResult<S> valid(S structure, List<String> warnings) {
        return new MultiblockValidationResult<>(true, Optional.of(structure), List.of(), List.copyOf(warnings));
    }

    public static <S> MultiblockValidationResult<S> invalid(List<String> errors, List<String> warnings) {
        return new MultiblockValidationResult<>(false, Optional.empty(), List.copyOf(errors), List.copyOf(warnings));
    }

    public String primaryMessage() {
        if (!errors.isEmpty()) {
            return errors.get(0);
        }
        if (!warnings.isEmpty()) {
            return warnings.get(0);
        }
        return formed ? "Structure formed" : "Structure incomplete";
    }
}
