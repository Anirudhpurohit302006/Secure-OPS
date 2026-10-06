package com.secureops.util;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Small JDBC helpers shared by the repositories. */
public final class Sql {
    private Sql() {}

    /** "?,?,?" for an IN clause. */
    public static String placeholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    public static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDateTime.class);
    }

    public static Double dbl(ResultSet rs, String col) throws SQLException {
        BigDecimal b = rs.getBigDecimal(col);
        return b == null ? null : b.doubleValue();
    }

    public static Long lng(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    public static Boolean bool(ResultSet rs, String col) throws SQLException {
        boolean v = rs.getBoolean(col);
        return rs.wasNull() ? null : v;
    }

    public static Integer integer(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /** Builds an argument array: ids first, then the extra arguments. */
    public static Object[] args(Collection<Integer> ids, Object... extra) {
        List<Object> a = new ArrayList<>(ids);
        a.addAll(List.of(extra));
        return a.toArray();
    }
}
