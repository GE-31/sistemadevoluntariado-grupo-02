-- ============================================================
-- Modulo Inventario - Sistema de Voluntariado
-- ============================================================

CREATE TABLE IF NOT EXISTS inventario_item (
    id_item INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    unidad_medida VARCHAR(30) NOT NULL,
    stock_actual DECIMAL(10,2) NOT NULL DEFAULT 0,
    stock_minimo DECIMAL(10,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    observacion VARCHAR(255) NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE inventario_item
    ADD UNIQUE KEY uk_inventario_item_nombre_categoria_unidad (nombre, categoria, unidad_medida);

CREATE INDEX idx_inventario_estado ON inventario_item(estado);
CREATE INDEX idx_inventario_categoria ON inventario_item(categoria);
CREATE INDEX idx_inventario_nombre ON inventario_item(nombre);

CREATE TABLE IF NOT EXISTS donacion_detalle (
    id_donacion_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_donacion INT NOT NULL,
    id_item INT NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    observacion VARCHAR(255) NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donacion_detalle_donacion FOREIGN KEY (id_donacion) REFERENCES donacion(id_donacion),
    CONSTRAINT fk_donacion_detalle_item FOREIGN KEY (id_item) REFERENCES inventario_item(id_item)
);

CREATE INDEX idx_donacion_detalle_donacion ON donacion_detalle(id_donacion);
CREATE INDEX idx_donacion_detalle_item ON donacion_detalle(id_item);

CREATE TABLE IF NOT EXISTS inventario_movimiento (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_item INT NOT NULL,
    tipo_movimiento VARCHAR(20) NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    stock_anterior DECIMAL(10,2) NOT NULL,
    stock_nuevo DECIMAL(10,2) NOT NULL,
    id_referencia INT NULL,
    tabla_referencia VARCHAR(40) NULL,
    observacion VARCHAR(255) NULL,
    id_usuario INT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimiento_item FOREIGN KEY (id_item) REFERENCES inventario_item(id_item)
);

CREATE INDEX idx_movimiento_item ON inventario_movimiento(id_item);
CREATE INDEX idx_movimiento_creado_en ON inventario_movimiento(creado_en);

ALTER TABLE donacion
    MODIFY cantidad DECIMAL(10,2) NULL;

ALTER TABLE donacion
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS anulado_en DATETIME NULL,
    ADD COLUMN IF NOT EXISTS id_usuario_anula INT NULL,
    ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS actualizado_en DATETIME NULL;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_listar_inventario $$
CREATE PROCEDURE sp_listar_inventario()
BEGIN
    SELECT id_item, nombre, categoria, unidad_medida, stock_actual, stock_minimo,
           estado, observacion, creado_en, actualizado_en
    FROM inventario_item
    ORDER BY creado_en DESC;
END $$

DROP PROCEDURE IF EXISTS sp_obtener_item_inventario $$
CREATE PROCEDURE sp_obtener_item_inventario(IN p_id_item INT)
BEGIN
    SELECT id_item, nombre, categoria, unidad_medida, stock_actual, stock_minimo,
           estado, observacion, creado_en, actualizado_en
    FROM inventario_item
    WHERE id_item = p_id_item;
END $$

DROP PROCEDURE IF EXISTS sp_crear_item_inventario $$
CREATE PROCEDURE sp_crear_item_inventario(
    IN p_nombre VARCHAR(150),
    IN p_categoria VARCHAR(50),
    IN p_unidad_medida VARCHAR(30),
    IN p_stock_minimo DECIMAL(10,2),
    IN p_observacion VARCHAR(255)
)
BEGIN
    INSERT INTO inventario_item(nombre, categoria, unidad_medida, stock_actual, stock_minimo, estado, observacion, creado_en, actualizado_en)
    VALUES(TRIM(p_nombre), UPPER(TRIM(p_categoria)), LOWER(TRIM(p_unidad_medida)), 0, IFNULL(p_stock_minimo, 0), 'ACTIVO', p_observacion, NOW(), NOW());

    SELECT LAST_INSERT_ID() AS id_item;
END $$

DROP PROCEDURE IF EXISTS sp_filtrar_inventario $$
CREATE PROCEDURE sp_filtrar_inventario(
    IN p_q VARCHAR(150),
    IN p_categoria VARCHAR(50),
    IN p_estado VARCHAR(20),
    IN p_stock_bajo TINYINT
)
BEGIN
    SELECT id_item, nombre, categoria, unidad_medida, stock_actual, stock_minimo,
           estado, observacion, creado_en, actualizado_en
    FROM inventario_item
    WHERE (p_q IS NULL OR TRIM(p_q) = '' OR LOWER(nombre) LIKE CONCAT('%', LOWER(TRIM(p_q)), '%')
           OR LOWER(COALESCE(observacion, '')) LIKE CONCAT('%', LOWER(TRIM(p_q)), '%'))
      AND (p_categoria IS NULL OR TRIM(p_categoria) = '' OR categoria = TRIM(p_categoria))
      AND (p_estado IS NULL OR TRIM(p_estado) = '' OR estado = UPPER(TRIM(p_estado)))
      AND (p_stock_bajo = 0 OR stock_actual <= stock_minimo)
    ORDER BY creado_en DESC;
END $$

DROP PROCEDURE IF EXISTS sp_contar_stock_bajo $$
CREATE PROCEDURE sp_contar_stock_bajo()
BEGIN
    SELECT COUNT(*) AS total
    FROM inventario_item
    WHERE estado = 'ACTIVO' AND stock_actual <= stock_minimo;
END $$

DROP PROCEDURE IF EXISTS sp_actualizar_item_inventario $$
CREATE PROCEDURE sp_actualizar_item_inventario(
    IN p_id_item INT,
    IN p_nombre VARCHAR(150),
    IN p_categoria VARCHAR(50),
    IN p_unidad_medida VARCHAR(30),
    IN p_stock_minimo DECIMAL(10,2),
    IN p_observacion VARCHAR(255)
)
BEGIN
    UPDATE inventario_item
    SET nombre = TRIM(p_nombre),
        categoria = UPPER(TRIM(p_categoria)),
        unidad_medida = LOWER(TRIM(p_unidad_medida)),
        stock_minimo = IFNULL(p_stock_minimo, 0),
        observacion = p_observacion,
        actualizado_en = NOW()
    WHERE id_item = p_id_item;

    SELECT ROW_COUNT() AS filas_afectadas;
END $$

DROP PROCEDURE IF EXISTS sp_registrar_movimiento_inventario $$
CREATE PROCEDURE sp_registrar_movimiento_inventario(
    IN p_id_item INT,
    IN p_tipo_movimiento VARCHAR(20),
    IN p_motivo VARCHAR(30),
    IN p_cantidad DECIMAL(10,2),
    IN p_observacion VARCHAR(255),
    IN p_id_usuario INT
)
BEGIN
    DECLARE v_stock_anterior DECIMAL(10,2) DEFAULT 0;
    DECLARE v_stock_nuevo DECIMAL(10,2) DEFAULT 0;
    DECLARE v_tipo VARCHAR(20);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_tipo = UPPER(TRIM(p_tipo_movimiento));

    IF p_id_item IS NULL OR p_id_item <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Debe seleccionar un item de inventario valido.';
    END IF;

    IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La cantidad del movimiento debe ser mayor a cero.';
    END IF;

    IF v_tipo NOT IN ('ENTRADA', 'SALIDA') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Tipo de movimiento invalido. Use ENTRADA o SALIDA.';
    END IF;

    START TRANSACTION;

    SELECT stock_actual
    INTO v_stock_anterior
    FROM inventario_item
    WHERE id_item = p_id_item
    FOR UPDATE;

    IF v_tipo = 'ENTRADA' THEN
        SET v_stock_nuevo = v_stock_anterior + p_cantidad;
    ELSE
        IF v_stock_anterior < p_cantidad THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Stock insuficiente para registrar la salida.';
        END IF;
        SET v_stock_nuevo = v_stock_anterior - p_cantidad;
    END IF;

    UPDATE inventario_item
    SET stock_actual = v_stock_nuevo,
        actualizado_en = NOW()
    WHERE id_item = p_id_item;

    INSERT INTO inventario_movimiento(
        id_item, tipo_movimiento, motivo, cantidad, stock_anterior, stock_nuevo,
        id_referencia, tabla_referencia, observacion, id_usuario, creado_en
    ) VALUES(
        p_id_item, v_tipo, UPPER(TRIM(IFNULL(p_motivo, 'MANUAL'))), p_cantidad, v_stock_anterior, v_stock_nuevo,
        NULL, NULL, p_observacion, p_id_usuario, NOW()
    );

    COMMIT;

    SELECT v_stock_nuevo AS stock_actual;
END $$

DROP PROCEDURE IF EXISTS sp_cambiar_estado_inventario $$
CREATE PROCEDURE sp_cambiar_estado_inventario(
    IN p_id_item INT,
    IN p_estado VARCHAR(20)
)
BEGIN
    UPDATE inventario_item
    SET estado = UPPER(p_estado),
        actualizado_en = NOW()
    WHERE id_item = p_id_item;

    SELECT ROW_COUNT() AS filas_afectadas;
END $$

DROP PROCEDURE IF EXISTS sp_registrar_donacion_inventario $$
CREATE PROCEDURE sp_registrar_donacion_inventario(
    IN p_cantidad DECIMAL(10,2),
    IN p_descripcion VARCHAR(150),
    IN p_id_tipo_donacion INT,
    IN p_id_actividad INT,
    IN p_id_usuario_registro INT,
    IN p_id_item INT,
    IN p_crear_nuevo_item TINYINT,
    IN p_item_nombre VARCHAR(150),
    IN p_item_categoria VARCHAR(50),
    IN p_item_unidad_medida VARCHAR(30),
    IN p_item_stock_minimo DECIMAL(10,2),
    IN p_donacion_anonima TINYINT,
    IN p_donante_tipo VARCHAR(20),
    IN p_donante_nombre VARCHAR(150),
    IN p_donante_correo VARCHAR(100),
    IN p_donante_telefono VARCHAR(30)
)
BEGIN
    DECLARE v_id_donacion INT;
    DECLARE v_id_item INT DEFAULT NULL;
    DECLARE v_id_donante INT DEFAULT NULL;
    DECLARE v_tipo_donante VARCHAR(20) DEFAULT NULL;
    DECLARE v_stock_anterior DECIMAL(10,2) DEFAULT 0;
    DECLARE v_stock_nuevo DECIMAL(10,2) DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La cantidad/monto de donacion debe ser mayor a cero.';
    END IF;

    INSERT INTO donacion(cantidad, descripcion, id_tipo_donacion, id_actividad, id_usuario_registro, registrado_en)
    VALUES(p_cantidad, p_descripcion, p_id_tipo_donacion, p_id_actividad, p_id_usuario_registro, NOW());

    SET v_id_donacion = LAST_INSERT_ID();

    IF IFNULL(p_donacion_anonima, 0) = 0 THEN
        IF p_donante_nombre IS NULL OR TRIM(p_donante_nombre) = '' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Debe indicar el nombre del donante o marcar donacion anonima.';
        END IF;

        SET v_tipo_donante = CASE UPPER(TRIM(IFNULL(p_donante_tipo, 'PERSONA')))
            WHEN 'EMPRESA' THEN 'Empresa'
            WHEN 'GRUPO' THEN 'Grupo'
            ELSE 'Persona'
        END;

        SELECT dnt.id_donante
        INTO v_id_donante
        FROM donante dnt
        WHERE LOWER(TRIM(dnt.nombre)) = LOWER(TRIM(p_donante_nombre))
          AND dnt.tipo = v_tipo_donante
          AND (
                IFNULL(TRIM(dnt.correo), '') = IFNULL(TRIM(p_donante_correo), '')
                OR IFNULL(TRIM(dnt.telefono), '') = IFNULL(TRIM(p_donante_telefono), '')
          )
        LIMIT 1;

        IF v_id_donante IS NULL THEN
            INSERT INTO donante(tipo, nombre, correo, telefono)
            VALUES(v_tipo_donante, TRIM(p_donante_nombre), NULLIF(TRIM(p_donante_correo), ''), NULLIF(TRIM(p_donante_telefono), ''));
            SET v_id_donante = LAST_INSERT_ID();
        END IF;

        INSERT INTO donacion_donante(id_donacion, id_donante)
        VALUES(v_id_donacion, v_id_donante);
    END IF;

    IF p_id_tipo_donacion = 2 THEN
        IF p_id_item IS NOT NULL AND p_id_item > 0 THEN
            SET v_id_item = p_id_item;
        ELSE
            IF p_item_nombre IS NULL OR TRIM(p_item_nombre) = '' OR
               p_item_categoria IS NULL OR TRIM(p_item_categoria) = '' OR
               p_item_unidad_medida IS NULL OR TRIM(p_item_unidad_medida) = '' THEN
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Debe seleccionar un item existente o completar datos para crear uno nuevo.';
            END IF;

            SELECT id_item
            INTO v_id_item
            FROM inventario_item
            WHERE LOWER(nombre) = LOWER(TRIM(p_item_nombre))
              AND LOWER(categoria) = LOWER(TRIM(p_item_categoria))
              AND LOWER(unidad_medida) = LOWER(TRIM(p_item_unidad_medida))
            LIMIT 1;

            IF v_id_item IS NULL AND p_crear_nuevo_item = 1 THEN
                INSERT INTO inventario_item(nombre, categoria, unidad_medida, stock_actual, stock_minimo, estado, observacion, creado_en, actualizado_en)
                VALUES(TRIM(p_item_nombre), UPPER(TRIM(p_item_categoria)), LOWER(TRIM(p_item_unidad_medida)),
                       0, IFNULL(p_item_stock_minimo, 0), 'ACTIVO',
                       CONCAT('Creado por donacion #', v_id_donacion), NOW(), NOW());
                SET v_id_item = LAST_INSERT_ID();
            END IF;
        END IF;

        IF v_id_item IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se pudo resolver el item de inventario para la donacion en especie.';
        END IF;

        SELECT stock_actual INTO v_stock_anterior
        FROM inventario_item
        WHERE id_item = v_id_item
        FOR UPDATE;

        SET v_stock_nuevo = v_stock_anterior + p_cantidad;

        UPDATE inventario_item
        SET stock_actual = v_stock_nuevo,
            actualizado_en = NOW()
        WHERE id_item = v_id_item;

        INSERT INTO donacion_detalle(id_donacion, id_item, cantidad, observacion, creado_en)
        VALUES(v_id_donacion, v_id_item, p_cantidad, p_descripcion, NOW());

        INSERT INTO inventario_movimiento(
            id_item, tipo_movimiento, motivo, cantidad, stock_anterior, stock_nuevo,
            id_referencia, tabla_referencia, observacion, id_usuario, creado_en
        ) VALUES(
            v_id_item, 'ENTRADA', 'DONACION', p_cantidad, v_stock_anterior, v_stock_nuevo,
            v_id_donacion, 'donacion', p_descripcion, p_id_usuario_registro, NOW()
        );
    END IF;

    COMMIT;

    SELECT v_id_donacion AS id_donacion;
END $$

DROP PROCEDURE IF EXISTS sp_listar_donaciones_con_detalle $$
CREATE PROCEDURE sp_listar_donaciones_con_detalle()
BEGIN
    SELECT
        d.id_donacion,
        d.cantidad,
        d.descripcion,
        td.nombre AS tipoDonacion,
        a.nombre AS actividad,
        CONCAT(u.nombres, ' ', u.apellidos) AS usuarioRegistro,
        COALESCE(dnt.nombre, 'ANONIMO') AS donanteNombre,
        d.registrado_en,
        d.id_tipo_donacion,
        d.id_actividad,
        ddet.id_item,
        ddet.cantidad AS cantidad_item,
        ii.nombre AS item_nombre,
        ii.unidad_medida AS item_unidad_medida,
        d.estado
    FROM donacion d
    LEFT JOIN tipo_donacion td ON d.id_tipo_donacion = td.id_tipo_donacion
    LEFT JOIN actividades a ON d.id_actividad = a.id_actividad
    LEFT JOIN usuario u ON d.id_usuario_registro = u.id_usuario
    LEFT JOIN donacion_donante ddon ON d.id_donacion = ddon.id_donacion
    LEFT JOIN donante dnt ON ddon.id_donante = dnt.id_donante
    LEFT JOIN donacion_detalle ddet ON d.id_donacion = ddet.id_donacion
    LEFT JOIN inventario_item ii ON ddet.id_item = ii.id_item
    WHERE COALESCE(d.estado, 'ACTIVO') = 'ACTIVO'
    ORDER BY d.registrado_en DESC;
END $$

DROP PROCEDURE IF EXISTS sp_obtener_donacion_detalle $$
CREATE PROCEDURE sp_obtener_donacion_detalle(IN p_id_donacion INT)
BEGIN
    SELECT
        d.id_donacion,
        d.cantidad,
        d.descripcion,
        d.id_tipo_donacion,
        td.nombre AS tipoDonacion,
        d.id_actividad,
        a.nombre AS actividad,
        d.id_usuario_registro,
        CONCAT(u.nombres, ' ', u.apellidos) AS usuarioRegistro,
        CASE WHEN ddon.id_donante IS NULL THEN 1 ELSE 0 END AS donacion_anonima,
        UPPER(COALESCE(dnt.tipo, 'PERSONA')) AS tipo_donante,
        dnt.nombre AS nombre_donante,
        dnt.correo AS correo_donante,
        dnt.telefono AS telefono_donante,
        ddet.id_item,
        ddet.cantidad AS cantidad_item,
        ii.nombre AS item_nombre,
        ii.categoria AS item_categoria,
        ii.unidad_medida AS item_unidad_medida,
        COALESCE(d.estado, 'ACTIVO') AS estado
    FROM donacion d
    LEFT JOIN tipo_donacion td ON d.id_tipo_donacion = td.id_tipo_donacion
    LEFT JOIN actividades a ON d.id_actividad = a.id_actividad
    LEFT JOIN usuario u ON d.id_usuario_registro = u.id_usuario
    LEFT JOIN donacion_donante ddon ON d.id_donacion = ddon.id_donacion
    LEFT JOIN donante dnt ON ddon.id_donante = dnt.id_donante
    LEFT JOIN donacion_detalle ddet ON d.id_donacion = ddet.id_donacion
    LEFT JOIN inventario_item ii ON ddet.id_item = ii.id_item
    WHERE d.id_donacion = p_id_donacion
    LIMIT 1;
END $$

DROP PROCEDURE IF EXISTS sp_actualizar_donacion_inventario $$
CREATE PROCEDURE sp_actualizar_donacion_inventario(
    IN p_id_donacion INT,
    IN p_cantidad DECIMAL(10,2),
    IN p_descripcion VARCHAR(150),
    IN p_id_actividad INT,
    IN p_donacion_anonima TINYINT,
    IN p_donante_tipo VARCHAR(20),
    IN p_donante_nombre VARCHAR(150),
    IN p_donante_correo VARCHAR(100),
    IN p_donante_telefono VARCHAR(30),
    IN p_id_usuario_edicion INT,
    IN p_motivo_edicion VARCHAR(255)
)
BEGIN
    DECLARE v_tipo INT;
    DECLARE v_id_donante INT DEFAULT NULL;
    DECLARE v_tipo_donante VARCHAR(20) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT id_tipo_donacion INTO v_tipo
    FROM donacion
    WHERE id_donacion = p_id_donacion
      AND COALESCE(estado, 'ACTIVO') = 'ACTIVO'
    FOR UPDATE;

    IF v_tipo IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La donacion no existe o ya fue anulada.';
    END IF;

    IF v_tipo = 1 THEN
        IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El monto para donaciones de dinero debe ser mayor a cero.';
        END IF;
    END IF;

    UPDATE donacion
    SET cantidad = CASE WHEN v_tipo = 1 THEN p_cantidad ELSE cantidad END,
        descripcion = p_descripcion,
        id_actividad = p_id_actividad,
        actualizado_en = NOW()
    WHERE id_donacion = p_id_donacion;

    IF IFNULL(p_donacion_anonima, 0) = 1 THEN
        DELETE FROM donacion_donante WHERE id_donacion = p_id_donacion;
    ELSE
        IF p_donante_nombre IS NULL OR TRIM(p_donante_nombre) = '' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Debe indicar el nombre del donante o marcar donacion anonima.';
        END IF;

        SET v_tipo_donante = CASE UPPER(TRIM(IFNULL(p_donante_tipo, 'PERSONA')))
            WHEN 'EMPRESA' THEN 'Empresa'
            WHEN 'GRUPO' THEN 'Grupo'
            ELSE 'Persona'
        END;

        SELECT dnt.id_donante
        INTO v_id_donante
        FROM donante dnt
        WHERE LOWER(TRIM(dnt.nombre)) = LOWER(TRIM(p_donante_nombre))
          AND dnt.tipo = v_tipo_donante
          AND (
                IFNULL(TRIM(dnt.correo), '') = IFNULL(TRIM(p_donante_correo), '')
                OR IFNULL(TRIM(dnt.telefono), '') = IFNULL(TRIM(p_donante_telefono), '')
          )
        LIMIT 1;

        IF v_id_donante IS NULL THEN
            INSERT INTO donante(tipo, nombre, correo, telefono)
            VALUES(v_tipo_donante, TRIM(p_donante_nombre), NULLIF(TRIM(p_donante_correo), ''), NULLIF(TRIM(p_donante_telefono), ''));
            SET v_id_donante = LAST_INSERT_ID();
        END IF;

        DELETE FROM donacion_donante WHERE id_donacion = p_id_donacion;
        INSERT INTO donacion_donante(id_donacion, id_donante)
        VALUES(p_id_donacion, v_id_donante);
    END IF;

    COMMIT;
END $$

DROP PROCEDURE IF EXISTS sp_anular_donacion_inventario $$
CREATE PROCEDURE sp_anular_donacion_inventario(
    IN p_id_donacion INT,
    IN p_id_usuario_anula INT,
    IN p_motivo VARCHAR(255)
)
BEGIN
    DECLARE v_tipo INT;
    DECLARE v_item INT;
    DECLARE v_cantidad DECIMAL(10,2);
    DECLARE v_stock_anterior DECIMAL(10,2) DEFAULT 0;
    DECLARE v_stock_nuevo DECIMAL(10,2) DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT id_tipo_donacion
    INTO v_tipo
    FROM donacion
    WHERE id_donacion = p_id_donacion
    FOR UPDATE;

    IF v_tipo IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La donacion no existe.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM donacion
        WHERE id_donacion = p_id_donacion
          AND COALESCE(estado, 'ACTIVO') = 'ANULADO'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La donacion ya esta anulada.';
    END IF;

    IF v_tipo = 2 THEN
        SELECT id_item, cantidad
        INTO v_item, v_cantidad
        FROM donacion_detalle
        WHERE id_donacion = p_id_donacion
        LIMIT 1;

        IF v_item IS NOT NULL AND v_cantidad IS NOT NULL AND v_cantidad > 0 THEN
            SELECT stock_actual INTO v_stock_anterior
            FROM inventario_item
            WHERE id_item = v_item
            FOR UPDATE;

            IF v_stock_anterior < v_cantidad THEN
                SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'No hay stock suficiente para revertir la donacion.';
            END IF;

            SET v_stock_nuevo = v_stock_anterior - v_cantidad;

            UPDATE inventario_item
            SET stock_actual = v_stock_nuevo,
                actualizado_en = NOW()
            WHERE id_item = v_item;

            INSERT INTO inventario_movimiento(
                id_item, tipo_movimiento, motivo, cantidad, stock_anterior, stock_nuevo,
                id_referencia, tabla_referencia, observacion, id_usuario, creado_en
            ) VALUES(
                v_item, 'SALIDA', 'ANULACION_DONACION', v_cantidad, v_stock_anterior, v_stock_nuevo,
                p_id_donacion, 'donacion', CONCAT('Anulacion de donacion #', p_id_donacion, '. ', IFNULL(p_motivo, '')), p_id_usuario_anula, NOW()
            );
        END IF;
    END IF;

    UPDATE donacion
    SET estado = 'ANULADO',
        anulado_en = NOW(),
        id_usuario_anula = p_id_usuario_anula,
        motivo_anulacion = LEFT(IFNULL(p_motivo, 'Anulacion manual'), 255),
        actualizado_en = NOW()
    WHERE id_donacion = p_id_donacion;

    COMMIT;
END $$

DELIMITER ;

-- Datos de ejemplo opcionales
INSERT INTO inventario_item(nombre, categoria, unidad_medida, stock_actual, stock_minimo, estado, observacion)
SELECT 'Leche evaporada', 'ALIMENTOS', 'lata', 60, 30, 'ACTIVO', 'Donacion de campaña enero'
WHERE NOT EXISTS (SELECT 1 FROM inventario_item WHERE nombre = 'Leche evaporada');

INSERT INTO inventario_item(nombre, categoria, unidad_medida, stock_actual, stock_minimo, estado, observacion)
SELECT 'Cuadernos A4', 'UTILES', 'unidad', 25, 40, 'ACTIVO', 'Stock bajo para proxima actividad'
WHERE NOT EXISTS (SELECT 1 FROM inventario_item WHERE nombre = 'Cuadernos A4');
