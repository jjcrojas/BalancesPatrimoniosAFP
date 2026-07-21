package co.gov.sfc.balancesafppatrimonios.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceDiario(
        int codigoEntidad,
        String nombreEntidad,
        String claveReporte,
        int codigoPuc,
        String nombreCuenta,
        LocalDate fecha,
        BigDecimal valorMiles
) {}
