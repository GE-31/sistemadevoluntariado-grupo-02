-- ============================================================
-- MÓDULO DE ASISTENCIAS - Sistema de Voluntariado
-- Tabla + Procedimientos Almacenados
-- ============================================================

-- ============================================================
-- 1. TABLA DE ASISTENCIAS
-- ============================================================
CREATE TABLE IF NOT EXISTS `asistencias` (
  `id_asistencia` int(11) NOT NULL AUTO_INCREMENT,
  `id_voluntario` int(11) NOT NULL,
  `id_actividad` int(11) NOT NULL,
  `fecha` date NOT NULL,
  `hora_entrada` time DEFAULT NULL,
  `hora_salida` time DEFAULT NULL,
  `horas_totales` decimal(5,2) DEFAULT 0.00,
  `estado` enum('ASISTIO','FALTA','TARDANZA') NOT NULL DEFAULT 'FALTA',
  `observaciones` text DEFAULT NULL,
  `id_usuario_registro` int(11) DEFAULT NULL COMMENT 'Usuario que registró la asistencia',
  `creado_en` timestamp NOT NULL DEFAULT current_timestamp(),
  `actualizado_en` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_asistencia`),
  UNIQUE KEY `uk_asistencia_voluntario_actividad_fecha` (`id_voluntario`, `id_actividad`, `fecha`),
  KEY `idx_asistencia_voluntario` (`id_voluntario`),
  KEY `idx_asistencia_actividad` (`id_actividad`),
  KEY `idx_asistencia_fecha` (`fecha`),
  KEY `idx_asistencia_estado` (`estado`),
  KEY `fk_asistencia_usuario` (`id_usuario_registro`),
  CONSTRAINT `fk_asistencia_voluntario` FOREIGN KEY (`id_voluntario`) REFERENCES `voluntario` (`id_voluntario`),
  CONSTRAINT `fk_asistencia_actividad` FOREIGN KEY (`id_actividad`) REFERENCES `actividades` (`id_actividad`),
  CONSTRAINT `fk_asistencia_usuario` FOREIGN KEY (`id_usuario_registro`) REFERENCES `usuario` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 2. PROCEDIMIENTOS ALMACENADOS
-- ============================================================

DELIMITER $$

-- --------------------------------------------------------------
-- 2.1  REGISTRAR ASISTENCIA
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_registrar_asistencia` (
    IN p_id_voluntario INT,
    IN p_id_actividad INT,
    IN p_fecha DATE,
    IN p_hora_entrada TIME,
    IN p_hora_salida TIME,
    IN p_estado VARCHAR(20),
    IN p_observaciones TEXT,
    IN p_id_usuario_registro INT
)
BEGIN
    DECLARE v_horas DECIMAL(5,2) DEFAULT 0.00;

    -- Calcular horas totales si ambas horas están presentes
    IF p_hora_entrada IS NOT NULL AND p_hora_salida IS NOT NULL THEN
        SET v_horas = ROUND(TIMESTAMPDIFF(MINUTE, p_hora_entrada, p_hora_salida) / 60.0, 2);
        IF v_horas < 0 THEN
            SET v_horas = 0.00;
        END IF;
    END IF;

    INSERT INTO asistencias (
        id_voluntario, id_actividad, fecha,
        hora_entrada, hora_salida, horas_totales,
        estado, observaciones, id_usuario_registro
    ) VALUES (
        p_id_voluntario, p_id_actividad, p_fecha,
        p_hora_entrada, p_hora_salida, v_horas,
        p_estado, p_observaciones, p_id_usuario_registro
    );

    SELECT LAST_INSERT_ID() AS id_asistencia;
END$$


-- --------------------------------------------------------------
-- 2.2  ACTUALIZAR ASISTENCIA
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_actualizar_asistencia` (
    IN p_id_asistencia INT,
    IN p_hora_entrada TIME,
    IN p_hora_salida TIME,
    IN p_estado VARCHAR(20),
    IN p_observaciones TEXT
)
BEGIN
    DECLARE v_horas DECIMAL(5,2) DEFAULT 0.00;

    IF p_hora_entrada IS NOT NULL AND p_hora_salida IS NOT NULL THEN
        SET v_horas = ROUND(TIMESTAMPDIFF(MINUTE, p_hora_entrada, p_hora_salida) / 60.0, 2);
        IF v_horas < 0 THEN
            SET v_horas = 0.00;
        END IF;
    END IF;

    UPDATE asistencias
    SET hora_entrada = p_hora_entrada,
        hora_salida  = p_hora_salida,
        horas_totales = v_horas,
        estado       = p_estado,
        observaciones = p_observaciones
    WHERE id_asistencia = p_id_asistencia;

    SELECT ROW_COUNT() AS filas_afectadas;
END$$


-- --------------------------------------------------------------
-- 2.3  LISTAR TODAS LAS ASISTENCIAS  (con nombres)
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_listar_asistencias` ()
BEGIN
    SELECT
        a.id_asistencia,
        a.id_voluntario,
        CONCAT(v.nombres, ' ', v.apellidos) AS nombre_voluntario,
        v.dni AS dni_voluntario,
        a.id_actividad,
        act.nombre AS nombre_actividad,
        a.fecha,
        a.hora_entrada,
        a.hora_salida,
        a.horas_totales,
        a.estado,
        a.observaciones,
        a.id_usuario_registro,
        CONCAT(u.nombres, ' ', u.apellidos) AS usuario_registro,
        a.creado_en
    FROM asistencias a
    INNER JOIN voluntario v   ON a.id_voluntario = v.id_voluntario
    INNER JOIN actividades act ON a.id_actividad  = act.id_actividad
    LEFT  JOIN usuario u       ON a.id_usuario_registro = u.id_usuario
    ORDER BY a.fecha DESC, a.creado_en DESC;
END$$


-- --------------------------------------------------------------
-- 2.4  OBTENER ASISTENCIA POR ID
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_obtener_asistencia_por_id` (
    IN p_id_asistencia INT
)
BEGIN
    SELECT
        a.id_asistencia,
        a.id_voluntario,
        CONCAT(v.nombres, ' ', v.apellidos) AS nombre_voluntario,
        v.dni AS dni_voluntario,
        a.id_actividad,
        act.nombre AS nombre_actividad,
        a.fecha,
        a.hora_entrada,
        a.hora_salida,
        a.horas_totales,
        a.estado,
        a.observaciones,
        a.id_usuario_registro,
        CONCAT(u.nombres, ' ', u.apellidos) AS usuario_registro,
        a.creado_en
    FROM asistencias a
    INNER JOIN voluntario v   ON a.id_voluntario = v.id_voluntario
    INNER JOIN actividades act ON a.id_actividad  = act.id_actividad
    LEFT  JOIN usuario u       ON a.id_usuario_registro = u.id_usuario
    WHERE a.id_asistencia = p_id_asistencia;
END$$


-- --------------------------------------------------------------
-- 2.5  LISTAR ASISTENCIAS POR ACTIVIDAD
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_listar_asistencias_por_actividad` (
    IN p_id_actividad INT
)
BEGIN
    SELECT
        a.id_asistencia,
        a.id_voluntario,
        CONCAT(v.nombres, ' ', v.apellidos) AS nombre_voluntario,
        v.dni AS dni_voluntario,
        a.id_actividad,
        act.nombre AS nombre_actividad,
        a.fecha,
        a.hora_entrada,
        a.hora_salida,
        a.horas_totales,
        a.estado,
        a.observaciones,
        a.creado_en
    FROM asistencias a
    INNER JOIN voluntario v   ON a.id_voluntario = v.id_voluntario
    INNER JOIN actividades act ON a.id_actividad  = act.id_actividad
    WHERE a.id_actividad = p_id_actividad
    ORDER BY a.fecha DESC, v.apellidos, v.nombres;
END$$


-- --------------------------------------------------------------
-- 2.6  LISTAR ASISTENCIAS POR VOLUNTARIO
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_listar_asistencias_por_voluntario` (
    IN p_id_voluntario INT
)
BEGIN
    SELECT
        a.id_asistencia,
        a.id_voluntario,
        CONCAT(v.nombres, ' ', v.apellidos) AS nombre_voluntario,
        a.id_actividad,
        act.nombre AS nombre_actividad,
        a.fecha,
        a.hora_entrada,
        a.hora_salida,
        a.horas_totales,
        a.estado,
        a.observaciones,
        a.creado_en
    FROM asistencias a
    INNER JOIN voluntario v   ON a.id_voluntario = v.id_voluntario
    INNER JOIN actividades act ON a.id_actividad  = act.id_actividad
    WHERE a.id_voluntario = p_id_voluntario
    ORDER BY a.fecha DESC;
END$$


-- --------------------------------------------------------------
-- 2.7  ELIMINAR ASISTENCIA
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_eliminar_asistencia` (
    IN p_id_asistencia INT
)
BEGIN
    DELETE FROM asistencias WHERE id_asistencia = p_id_asistencia;
    SELECT ROW_COUNT() AS filas_afectadas;
END$$


-- --------------------------------------------------------------
-- 2.8  ESTADÍSTICAS DE ASISTENCIA
-- --------------------------------------------------------------
CREATE PROCEDURE `sp_estadisticas_asistencias` ()
BEGIN
    SELECT
        COUNT(*) AS total_registros,
        SUM(CASE WHEN estado = 'ASISTIO' THEN 1 ELSE 0 END) AS total_asistieron,
        SUM(CASE WHEN estado = 'FALTA' THEN 1 ELSE 0 END) AS total_faltas,
        SUM(CASE WHEN estado = 'TARDANZA' THEN 1 ELSE 0 END) AS total_tardanzas,
        IFNULL(SUM(horas_totales), 0) AS total_horas,
        COUNT(DISTINCT id_voluntario) AS voluntarios_unicos,
        COUNT(DISTINCT id_actividad) AS actividades_registradas
    FROM asistencias;
END$$


DELIMITER ;
