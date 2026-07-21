package co.gov.sfc.balancesafppatrimonios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ReporteForm {

    @NotNull(message = "Debe seleccionar la fecha de corte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaCorte;

    @NotBlank(message = "Debe indicar la ruta base de salida")
    private String rutaBaseSalida;

    public LocalDate getFechaCorte() {
        return fechaCorte;
    }

    public void setFechaCorte(LocalDate fechaCorte) {
        this.fechaCorte = fechaCorte;
    }

    public String getRutaBaseSalida() {
        return rutaBaseSalida;
    }

    public void setRutaBaseSalida(String rutaBaseSalida) {
        this.rutaBaseSalida = rutaBaseSalida;
    }
}
