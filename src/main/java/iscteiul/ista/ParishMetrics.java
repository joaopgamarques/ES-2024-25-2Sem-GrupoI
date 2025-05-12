package iscteiul.ista;

/**
 * Immutable value-object representing one row from {@code parish-metrics.csv}.
 *
 * @param parishName                official parish (freguesia) name
 * @param distanceAirportKm         straight-line distance to Madeira airport (km)
 * @param distanceFunchalSeKm       distance to Funchal (Sé) city centre (km)
 * @param distanceFunchalPortKm     distance to Funchal port (km)
 * @param averagePriceEuroM2        average real-estate asking price (€/m²)
 * @param populationDensityHabKm2   population density (inhabitants/km²)
 * @param infrastructureQualityIdx  quality index (1=poor … 5=excellent)
 */
public record ParishMetrics(
        String parishName,
        double distanceAirportKm,
        double distanceFunchalSeKm,
        double distanceFunchalPortKm,
        double averagePriceEuroM2,
        int    populationDensityHabKm2,
        int    infrastructureQualityIdx
) {}
