-- Consulta parametrizable usada conceptualmente por la aplicación.
-- :fechaInicial, :fechaCorte y :codigosPuc se suministran desde Java.

SELECT
    e.Codigo_Entidad,
    TRIM(e.Nombre_Entidad) AS Nombre_Entidad,
    CASE
        WHEN e.Codigo_Entidad = 9
         AND pa.Tipo_Patrimonio = 6
         AND pa.Codigo_Patrimonio IN (4, 8000)
            THEN 'SKANDIA_ALT'
        ELSE 'ENT_' || TRIM(CAST(e.Codigo_Entidad AS VARCHAR(20)))
    END AS Clave_Reporte,
    p.Codigo AS Codigo_PUC,
    TRIM(p.Nombre) AS Nombre_Cuenta,
    t.Fecha,
    SUM(eip.Saldo_Sincierre_Total_Moneda_0) / 1000 AS Valor_Miles
FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip
INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e
    ON eip.Ent_ID = e.Ent_ID
INNER JOIN PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa
    ON eip.Paau_ID = pa.Paau_ID
INNER JOIN PROD_DWH_CONSULTA.TIEMPO t
    ON eip.Tie_ID = t.Tie_ID
INNER JOIN PROD_DWH_CONSULTA.PUC p
    ON eip.Puc_ID = p.Puc_ID
WHERE eip.Tipo_Informe = 17
  AND e.Tipo_Entidad = 23
  AND e.Estado = 1
  AND pa.Tipo_Patrimonio = 6
  AND p.Codigo IN (100000,110000,110500,110505,110510,110515,110520)
  AND t.Fecha BETWEEN DATE '2026-05-01' AND DATE '2026-05-31'
  AND (
        (
            e.Codigo_Entidad <> 9
            AND pa.Codigo_Patrimonio IN (1000,5000,6000,7000,8000)
        )
        OR
        (
            e.Codigo_Entidad = 9
            AND pa.Codigo_Patrimonio IN (1000,5000,6000,7000)
        )
        OR
        (
            e.Codigo_Entidad = 9
            AND pa.Codigo_Patrimonio IN (4,8000)
        )
  )
GROUP BY 1,2,3,4,5,6
ORDER BY 1,3,4,6;
