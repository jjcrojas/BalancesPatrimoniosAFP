# BalancesAFPPatrimonios

Aplicación Spring Boot para reemplazar gradualmente el proceso legado:

```text
Ruta SPSS → archivos TXT → FoxPro → DBF → Access → Excel
```

La aplicación consulta directamente Teradata y genera un archivo Excel con:

- hoja `restot`;
- una hoja por cada entidad vigente de `ENTIDADES`;
- hoja `SKANDIA_ALT` cuando la entidad 9 está vigente;
- columnas diarias `D1` a `D31`;
- total del sistema en `restot`.

La descarga agrupa en un ZIP los reportes de Sistema Total, Cesantías Corto
Plazo, Cesantías Largo Plazo, Cesantías Total, Conservador y Mayor Riesgo. El
reporte Conservador filtra el tipo de patrimonio `6` y el código `5000`; Mayor
Riesgo utiliza el mismo tipo de patrimonio y el código `6000`.

## Normalización de nombres de entidades

La aplicación elimina las comillas dobles que puedan estar almacenadas en
`ENTIDADES.Nombre_Entidad`. La limpieza se realiza en la consulta SQL y se
refuerza en Java antes de crear encabezados y nombres de hojas.

## Regla de entidades dinámicas

Se consultan las entidades que cumplen:

```sql
e.Tipo_Entidad = 23
AND e.Estado = 1
```

Por lo tanto, los códigos de entidad no están quemados en la aplicación.

## Regla de Skandia Alternativo

Para la entidad 9:

```sql
pa.Tipo_Patrimonio = 6
AND pa.Codigo_Patrimonio IN (4, 8000)
```

se genera la clave lógica y la hoja:

```text
SKANDIA_ALT
```

El patrimonio `8000` se excluye de la hoja principal de Skandia para evitar duplicarlo.

## Mejora respecto del query inicial

El query inicial mezclaba clasificación `CES`, `OBL` y `CYV`, y usaba códigos como
`10000`, `60000`, `70000` y `80000`. Para este producto solo se necesita el saldo
de pensiones obligatorias. La consulta implementada:

- exige `Tipo_Patrimonio = 6`;
- usa los códigos ordinarios `1000, 5000, 6000, 7000, 8000`;
- separa correctamente `4, 8000` de Skandia;
- filtra entidades vigentes;
- agrupa por entidad lógica, cuenta y fecha;
- consulta todo el mes hasta la fecha de corte.

## Configuración

Variables recomendadas en PowerShell:

```powershell
$env:BALANCES_AFP_PATRIMONIOS_DB_USER="usuario"
$env:BALANCES_AFP_PATRIMONIOS_DB_PASSWORD="clave"
```

Ejecución:

```powershell
mvn clean spring-boot:run
```

Abrir:

```text
http://localhost:8086
```

Compilación:

```powershell
mvn clean package
java -jar target\balances-afp-patrimonios-0.0.1-SNAPSHOT.jar
```

## Cuentas iniciales

```text
100000
110000
110500
110505
110510
110515
110520
```

Se pueden ampliar desde `application.properties` sin modificar Java.

## Validaciones recomendadas

Comparar, para uno o varios meses:

- valores D1 a D31;
- total por cuenta y entidad;
- total del sistema;
- saldo principal de Skandia;
- saldo de `SKANDIA_ALT`;
- ausencia de duplicidad del patrimonio 8000.
