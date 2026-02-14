-- ============================================================
-- PROCEDIMIENTOS PARA DASHBOARD - Gráficos
-- ============================================================

DELIMITER $$

-- --------------------------------------------------------------
-- Actividades por mes (últimos 6 meses)
-- Retorna: mes (YYYY-MM), nombre_mes, total_actividades
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_actividades_por_mes` ()
BEGIN
    SELECT 
        DATE_FORMAT(m.mes, '%Y-%m') AS mes,
        DATE_FORMAT(m.mes, '%b') AS nombre_mes,
        IFNULL(COUNT(a.id_actividad), 0) AS total_actividades
    FROM (
        SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n MONTH), '%Y-%m-01') AS mes
        FROM (
            SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 
            UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        ) nums
    ) m
    LEFT JOIN actividades a 
        ON DATE_FORMAT(a.fecha_inicio, '%Y-%m') = DATE_FORMAT(m.mes, '%Y-%m')
    GROUP BY m.mes
    ORDER BY m.mes ASC;
END$$

-- --------------------------------------------------------------
-- Horas voluntarias por actividad (top 5 actividades con más horas)
-- Retorna: nombre_actividad, total_horas
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_horas_voluntarias_por_actividad` ()
BEGIN
    SELECT 
        act.nombre AS nombre_actividad,
        IFNULL(SUM(a.horas_totales), 0) AS total_horas
    FROM asistencias a
    INNER JOIN actividades act ON a.id_actividad = act.id_actividad
    WHERE a.estado IN ('ASISTIO', 'TARDANZA')
    GROUP BY act.id_actividad, act.nombre
    ORDER BY total_horas DESC
    LIMIT 5;
END$$

-- --------------------------------------------------------------
-- Total de horas voluntarias globales
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_total_horas_voluntarias` ()
BEGIN
    SELECT IFNULL(SUM(horas_totales), 0) AS total_horas
    FROM asistencias
    WHERE estado IN ('ASISTIO', 'TARDANZA');
END$$

-- --------------------------------------------------------------
-- Próxima actividad (la más cercana en el futuro)
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_proxima_actividad` ()
BEGIN
    SELECT 
        id_actividad,
        nombre,
        fecha_inicio,
        ubicacion
    FROM actividades
    WHERE fecha_inicio >= CURDATE()
      AND estado = 'ACTIVO'
    ORDER BY fecha_inicio ASC
    LIMIT 1;
END$$

DELIMITER ;
