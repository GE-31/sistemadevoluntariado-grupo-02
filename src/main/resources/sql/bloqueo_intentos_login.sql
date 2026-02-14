-- ============================================================
-- Script para agregar bloqueo por intentos fallidos de login
-- Máximo 3 intentos fallidos → bloqueo de 15 minutos
-- ============================================================

-- Agregar columnas a la tabla usuario (solo si no existen)
SET @dbname = DATABASE();

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'intentos_fallidos') > 0,
    'SELECT 1',
    'ALTER TABLE `usuario` ADD COLUMN `intentos_fallidos` INT NOT NULL DEFAULT 0 AFTER `actualizado_en`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'bloqueado_hasta') > 0,
    'SELECT 1',
    'ALTER TABLE `usuario` ADD COLUMN `bloqueado_hasta` DATETIME NULL DEFAULT NULL AFTER `intentos_fallidos`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Inicializar valores para usuarios existentes
UPDATE `usuario` SET `intentos_fallidos` = 0, `bloqueado_hasta` = NULL;

-- ============================================================
-- Procedimientos almacenados para login y bloqueo
-- ============================================================

DELIMITER $$

-- SP: Verificar si la cuenta está bloqueada
-- Retorna: minutos_restantes (0 si no está bloqueada)
DROP PROCEDURE IF EXISTS `sp_verificar_bloqueo`$$
CREATE PROCEDURE `sp_verificar_bloqueo` (IN `p_username` VARCHAR(60))
BEGIN
    SELECT intentos_fallidos, bloqueado_hasta
    FROM usuario
    WHERE username = p_username;
END$$

-- SP: Registrar intento fallido de login
-- Incrementa intentos y si alcanza el máximo (3), bloquea por 15 minutos
-- Retorna: intentos_fallidos actuales
DROP PROCEDURE IF EXISTS `sp_registrar_intento_fallido`$$
CREATE PROCEDURE `sp_registrar_intento_fallido` (
    IN `p_username` VARCHAR(60),
    IN `p_max_intentos` INT,
    IN `p_tiempo_bloqueo_minutos` INT
)
BEGIN
    -- Incrementar intentos fallidos
    UPDATE usuario
    SET intentos_fallidos = intentos_fallidos + 1
    WHERE username = p_username;

    -- Si alcanzó el máximo, bloquear la cuenta
    UPDATE usuario
    SET bloqueado_hasta = DATE_ADD(NOW(), INTERVAL p_tiempo_bloqueo_minutos MINUTE)
    WHERE username = p_username
      AND intentos_fallidos >= p_max_intentos;

    -- Retornar intentos actuales
    SELECT intentos_fallidos
    FROM usuario
    WHERE username = p_username;
END$$

-- SP: Resetear intentos fallidos (login exitoso o bloqueo expirado)
DROP PROCEDURE IF EXISTS `sp_resetear_intentos_fallidos`$$
CREATE PROCEDURE `sp_resetear_intentos_fallidos` (IN `p_username` VARCHAR(60))
BEGIN
    UPDATE usuario
    SET intentos_fallidos = 0,
        bloqueado_hasta = NULL
    WHERE username = p_username;
END$$

-- SP: Obtener intentos restantes antes del bloqueo
DROP PROCEDURE IF EXISTS `sp_obtener_intentos_restantes`$$
CREATE PROCEDURE `sp_obtener_intentos_restantes` (
    IN `p_username` VARCHAR(60),
    IN `p_max_intentos` INT
)
BEGIN
    SELECT (p_max_intentos - COALESCE(intentos_fallidos, 0)) AS intentos_restantes
    FROM usuario
    WHERE username = p_username;
END$$

-- SP: Obtener datos de usuario por username (para validar login)
DROP PROCEDURE IF EXISTS `sp_obtener_usuario_por_username`$$
CREATE PROCEDURE `sp_obtener_usuario_por_username` (IN `p_username` VARCHAR(60))
BEGIN
    SELECT id_usuario, nombres, apellidos, correo, username, dni,
           password_hash, foto_perfil, estado, creado_en, actualizado_en
    FROM usuario
    WHERE username = p_username;
END$$

DELIMITER ;
