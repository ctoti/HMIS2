package com.pha.hr.shared;

import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * The fields an endpoint may be sorted by. Checked up front so an unknown field is a 400, not a query error.
 */
public record SortableFields(Set<String> names) {

    public SortableFields {
        names = Set.copyOf(names);
    }

    public static SortableFields of(String... names) {
        return new SortableFields(Set.of(names));
    }

    public Pageable check(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!names.contains(order.getProperty())) {
                throw new InvalidSortException(new TreeSet<>(names));
            }
        }
        return pageable;
    }

    public static class InvalidSortException extends RuntimeException {

        InvalidSortException(Set<String> allowed) {
            super("Sort by one of: " + String.join(", ", allowed) + ".");
        }
    }
}
