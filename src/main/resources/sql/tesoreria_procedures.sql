-- ============================================================
-- MÓDULO DE TESORERÍA - Sistema de Voluntariado
-- Controla ingresos, gastos y balance financiero
-- ============================================================

-- Tabla principal de movimientos financieros
CREATE TABLE IF NOT EXISTS `movimiento_financiero` (
    `id_movimiento`     INT(11)         NOT NULL AUTO_INCREMENT,
    `tipo`              ENUM('INGRESO','GASTO') NOT NULL,
    `monto`             DECIMAL(12,2)   NOT NULL,
    `descripcion`       VARCHAR(255)    NOT NULL,
    `categoria`         VARCHAR(60)     NOT NULL,
    `comprobante`       VARCHAR(100)    DEFAULT NULL,
    `fecha_movimiento`  DATE            NOT NULL,
    `id_actividad`      INT(11)         DEFAULT NULL,
    `id_usuario`        INT(11)         NOT NULL,
    `creado_en`         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id_movimiento`),
    KEY `fk_mov_actividad` (`id_actividad`),
    KEY `fk_mov_usuario`   (`id_usuario`),
    CONSTRAINT `fk_mov_actividad` FOREIGN KEY (`id_actividad`)
        REFERENCES `actividades`(`id_actividad`) ON DELETE SET NULL,
    CONSTRAINT `fk_mov_usuario`   FOREIGN KEY (`id_usuario`)
        REFERENCES `usuario`(`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS
-- ============================================================

DELIMITER $$

-- 1. Listar todos los movimientos (más recientes primero)
CREATE PROCEDURE `sp_listarMovimientos`()
BEGIN
    SELECT m.id_movimiento, m.tipo, m.monto, m.descripcion,
           m.categoria, m.comprobante, m.fecha_movimiento,
           IFNULL(a.nombre, '—') AS actividad,
           m.id_actividad,
           CONCAT(u.nombres, ' ', u.apellidos) AS usuario_registro,
           m.creado_en
    FROM movimiento_financiero m
    INNER JOIN usuario u ON m.id_usuario = u.id_usuario
    LEFT  JOIN actividades a ON m.id_actividad = a.id_actividad
    ORDER BY m.fecha_movimiento DESC, m.creado_en DESC;
END$$

-- 2. Obtener un movimiento por ID
CREATE PROCEDURE `sp_obtenerMovimiento`(IN p_id INT)
BEGIN
    SELECT m.id_movimiento, m.tipo, m.monto, m.descripcion,
           m.categoria, m.comprobante, m.fecha_movimiento,
           m.id_actividad, m.id_usuario, m.creado_en
    FROM movimiento_financiero m
    WHERE m.id_movimiento = p_id;
END$$

-- 3. Registrar nuevo movimiento
CREATE PROCEDURE `sp_registrarMovimiento`(
    IN p_tipo           VARCHAR(10),
    IN p_monto          DECIMAL(12,2),
    IN p_descripcion    VARCHAR(255),
    IN p_categoria      VARCHAR(60),
    IN p_comprobante    VARCHAR(100),
    IN p_fecha          DATE,
    IN p_id_actividad   INT,
    IN p_id_usuario     INT
)
BEGIN
    INSERT INTO movimiento_financiero
        (tipo, monto, descripcion, categoria, comprobante,
         fecha_movimiento, id_actividad, id_usuario)
    VALUES
        (p_tipo, p_monto, p_descripcion, p_categoria, p_comprobante,
         p_fecha, NULLIF(p_id_actividad, 0), p_id_usuario);

    SELECT LAST_INSERT_ID() AS id_movimiento;
END$$

-- 4. Actualizar movimiento
CREATE PROCEDURE `sp_actualizarMovimiento`(
    IN p_id             INT,
    IN p_tipo           VARCHAR(10),
    IN p_monto          DECIMAL(12,2),
    IN p_descripcion    VARCHAR(255),
    IN p_categoria      VARCHAR(60),
    IN p_comprobante    VARCHAR(100),
    IN p_fecha          DATE,
    IN p_id_actividad   INT
)
BEGIN
    UPDATE movimiento_financiero
    SET tipo              = p_tipo,
        monto             = p_monto,
        descripcion       = p_descripcion,
        categoria         = p_categoria,
        comprobante       = p_comprobante,
        fecha_movimiento  = p_fecha,
        id_actividad      = NULLIF(p_id_actividad, 0)
    WHERE id_movimiento   = p_id;

    SELECT ROW_COUNT() AS filas_afectadas;
END$$

-- 5. Eliminar movimiento
CREATE PROCEDURE `sp_eliminarMovimiento`(IN p_id INT)
BEGIN
    DELETE FROM movimiento_financiero WHERE id_movimiento = p_id;
    SELECT ROW_COUNT() AS filas_afectadas;
END$$

-- 6. Balance general (total ingresos, total gastos, saldo)
CREATE PROCEDURE `sp_obtenerBalance`()
BEGIN
    SELECT
        IFNULL(SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE 0 END), 0) AS total_ingresos,
        IFNULL(SUM(CASE WHEN tipo = 'GASTO'   THEN monto ELSE 0 END), 0) AS total_gastos,
        IFNULL(SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE -monto END), 0) AS saldo
    FROM movimiento_financiero;
END$$

-- 7. Filtrar movimientos (por tipo, categoría, rango de fechas)
CREATE PROCEDURE `sp_filtrarMovimientos`(
    IN p_tipo       VARCHAR(10),
    IN p_categoria  VARCHAR(60),
    IN p_fecha_ini  DATE,
    IN p_fecha_fin  DATE
)
BEGIN
    SELECT m.id_movimiento, m.tipo, m.monto, m.descripcion,
           m.categoria, m.comprobante, m.fecha_movimiento,
           IFNULL(a.nombre, '—') AS actividad,
           m.id_actividad,
           CONCAT(u.nombres, ' ', u.apellidos) AS usuario_registro,
           m.creado_en
    FROM movimiento_financiero m
    INNER JOIN usuario u ON m.id_usuario = u.id_usuario
    LEFT  JOIN actividades a ON m.id_actividad = a.id_actividad
    WHERE (p_tipo IS NULL      OR p_tipo = ''      OR m.tipo = p_tipo)
      AND (p_categoria IS NULL OR p_categoria = '' OR m.categoria = p_categoria)
      AND (p_fecha_ini IS NULL OR m.fecha_movimiento >= p_fecha_ini)
      AND (p_fecha_fin IS NULL OR m.fecha_movimiento <= p_fecha_fin)
    ORDER BY m.fecha_movimiento DESC, m.creado_en DESC;
END$$

-- 8. Resumen por categoría (para gráficos)
CREATE PROCEDURE `sp_resumenPorCategoria`()
BEGIN
    SELECT categoria, tipo,
           SUM(monto) AS total,
           COUNT(*)   AS cantidad
    FROM movimiento_financiero
    GROUP BY categoria, tipo
    ORDER BY total DESC;
END$$

-- 9. Resumen mensual (para gráficos de tendencia)
CREATE PROCEDURE `sp_resumenMensual`()
BEGIN
    SELECT
        DATE_FORMAT(fecha_movimiento, '%Y-%m') AS mes,
        SUM(CASE WHEN tipo = 'INGRESO' THEN monto ELSE 0 END) AS ingresos,
        SUM(CASE WHEN tipo = 'GASTO'   THEN monto ELSE 0 END) AS gastos
    FROM movimiento_financiero
    GROUP BY DATE_FORMAT(fecha_movimiento, '%Y-%m')
    ORDER BY mes DESC
    LIMIT 12;
END$$

DELIMITER ;
