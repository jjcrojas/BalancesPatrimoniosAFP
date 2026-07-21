package co.gov.sfc.balancesafppatrimonios.model;

public record EntidadReporte(
        int codigoEntidad,
        String nombreEntidad,
        String claveReporte,
        String nombreHoja,
        boolean virtual
) {}
