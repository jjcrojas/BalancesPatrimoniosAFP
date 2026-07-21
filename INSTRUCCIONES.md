# Cesantías Largo Plazo

Este parche agrega `ExcelCesantiasLargoPlazoService.java` y el SQL de referencia.

## 1. application.properties

Agregue:

```properties
balances-afp-patrimonios.tipo-patrimonio-cesantias-largo-plazo=5
balances-afp-patrimonios.codigo-patrimonio-cesantias-largo-plazo=1
```

## 2. BalancesAfpPatrimoniosProperties.java

Agregue en la clase principal:

```java
@NotNull
private Integer tipoPatrimonioCesantiasLargoPlazo;

@NotNull
private Integer codigoPatrimonioCesantiasLargoPlazo;

public Integer getTipoPatrimonioCesantiasLargoPlazo() {
    return tipoPatrimonioCesantiasLargoPlazo;
}

public void setTipoPatrimonioCesantiasLargoPlazo(
        Integer tipoPatrimonioCesantiasLargoPlazo) {
    this.tipoPatrimonioCesantiasLargoPlazo =
            tipoPatrimonioCesantiasLargoPlazo;
}

public Integer getCodigoPatrimonioCesantiasLargoPlazo() {
    return codigoPatrimonioCesantiasLargoPlazo;
}

public void setCodigoPatrimonioCesantiasLargoPlazo(
        Integer codigoPatrimonioCesantiasLargoPlazo) {
    this.codigoPatrimonioCesantiasLargoPlazo =
            codigoPatrimonioCesantiasLargoPlazo;
}
```

## 3. BalancePatrimoniosRepository.java

Agregue `SQL_CESANTIAS_LARGO_PLAZO`, equivalente al SQL incluido en `sql/consulta_cesantias_largo_plazo_afp.sql`, usando parámetros:

```sql
AND pa.Tipo_Patrimonio = :tipoPatrimonioCesantias
AND pa.Codigo_Patrimonio = :codigoPatrimonioCesantias
```

Agregue:

```java
public List<BalanceDiario> consultarCesantiasLargoPlazo(
        LocalDate fechaCorte) {

    LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

    MapSqlParameterSource params =
            parametrosComunes(fechaInicial, fechaCorte)
                    .addValue(
                            "tipoPatrimonioCesantias",
                            properties
                                    .getTipoPatrimonioCesantiasLargoPlazo()
                    )
                    .addValue(
                            "codigoPatrimonioCesantias",
                            properties
                                    .getCodigoPatrimonioCesantiasLargoPlazo()
                    );

    return ejecutarBalances(
            "Cesantías Largo Plazo",
            SQL_CESANTIAS_LARGO_PLAZO,
            params
    );
}
```

Conserve el método de reintento por timeout que ya tiene el repositorio.

## 4. Optimización de entidades

Consulte las entidades vigentes una sola vez:

```java
List<EntidadReporte> entidadesFisicas =
        repository.consultarEntidadesFisicas();
```

Reutilice `entidadesFisicas` para Cesantías Corto y Largo Plazo.
Para Sistema Total cree una copia y agregue `SKANDIA_ALT` en memoria:

```java
List<EntidadReporte> entidadesSistemaTotal =
        repository.construirEntidadesSistemaTotal(entidadesFisicas);
```

Así no se vuelve a consultar `ENTIDADES` para cada archivo.

## 5. ReporteController.java

Inyecte:

```java
private final ExcelCesantiasLargoPlazoService
        excelCesantiasLargoPlazoService;
```

En el constructor agregue el servicio y asígnelo.

En `generar(...)`, después de corto plazo:

```java
List<BalanceDiario> cesantiasLargoPlazo =
        repository.consultarCesantiasLargoPlazo(fechaCorte);

ExcelCesantiasLargoPlazoService.GeneratedReport largoPlazo =
        excelCesantiasLargoPlazoService.generar(
                fechaCorte,
                rutaBaseSalida,
                entidadesFisicas,
                cuentas,
                cesantiasLargoPlazo
        );

registrarGuardado(largoPlazo.archivoGuardado());
```

Agregue `largoPlazo` a la lista del ZIP:

```java
new ReportesZipService.ArchivoZip(
        largoPlazo.fileName(),
        largoPlazo.content()
)
```

El archivo resultante será:

```text
CESANTIAS LARGO PLAZO Mayo 2026.xlsx
```

No se crea hoja `SKANDIA_ALT` en ninguno de los dos archivos de cesantías.
