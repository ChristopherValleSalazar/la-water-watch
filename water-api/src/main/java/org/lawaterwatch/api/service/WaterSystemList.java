package org.lawaterwatch.api.service;

import java.util.List;

//The 11 LA County public water systems this app covers, the only place they are hardcoded
public final class WaterSystemList {

    public record Entry(String name, String pwsid) {}

    public static final List<Entry> ALL = List.of(
            new Entry("LADWP", "CA1910067"),
            new Entry("Compton", "CA1910026"),
            new Entry("Lynwood", "CA1910079"),
            new Entry("Inglewood", "CA1910051"),
            new Entry("Beverly Hills", "CA1910156"),
            new Entry("South Gate", "CA1910152"),
            new Entry("Paramount", "CA1910105"),
            new Entry("Santa Monica", "CA1910146"),
            new Entry("Pasadena", "CA1910124"),
            new Entry("Burbank", "CA1910179"),
            new Entry("Glendale", "CA1910043")
    );

    public static final List<String> PWSIDS = ALL.stream().map(Entry::pwsid).toList();

    private WaterSystemList() {}

    public static boolean contains(String pwsid) {
        return PWSIDS.contains(pwsid);
    }
}
