-- Moderado
SELECT
    e.Codigo_Entidad AS codigo_entidad,
    TRIM(OREPLACE(e.Nombre_Entidad, '"', '')) AS nombre_entidad,
    'ENT_' || TRIM(CAST(e.Codigo_Entidad AS VARCHAR(20))) AS clave_reporte,
    p.Codigo AS codigo_puc,
    TRIM(p.Nombre) AS nombre_cuenta,
    t.Fecha AS fecha,
    SUM(eip.Saldo_Sincierre_Total_Moneda_0) / 1000 AS valor_miles
FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip
INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e ON eip.Ent_ID = e.Ent_ID
INNER JOIN PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa ON eip.Paau_ID = pa.Paau_ID
INNER JOIN PROD_DWH_CONSULTA.TIEMPO t ON eip.Tie_ID = t.Tie_ID
INNER JOIN PROD_DWH_CONSULTA.PUC p ON eip.Puc_ID = p.Puc_ID
WHERE eip.Tipo_Informe = 17
  AND e.Tipo_Entidad = 23
  AND e.Estado = 1
  AND pa.Tipo_Patrimonio = 6
  AND pa.Codigo_Patrimonio = 1000
  AND p.Codigo IN (100000, 110000, 110500, 110505, 110510, 110515, 110520)
  AND t.Fecha BETWEEN '2026-06-01' AND '2026-06-30'
GROUP BY 1, 2, 3, 4, 5, 6
ORDER BY 1, 4, 6;
