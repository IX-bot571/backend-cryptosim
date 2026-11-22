-- Ajustes que ddl-auto=update de Hibernate no hace sobre una tabla existente. Idempotente: se ejecuta en cada
-- arranque, después de Hibernate (spring.jpa.defer-datasource-initialization). Separador: ver spring.sql.init.separator
-- 1) El CHECK de tipo se creó con los dos tipos originales: se rehace solo si no incluye VARIACION_VENTANA.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conname = 'alertas_tipo_check'
                 AND pg_get_constraintdef(oid) NOT LIKE '%VARIACION_VENTANA%') THEN
        ALTER TABLE alertas DROP CONSTRAINT alertas_tipo_check;
        ALTER TABLE alertas ADD CONSTRAINT alertas_tipo_check
            CHECK (tipo IN ('PRECIO_MAYOR_IGUAL', 'PRECIO_MENOR_IGUAL', 'VARIACION_VENTANA'));
    END IF;
END $$
@@
-- 2) valor deja de ser obligatorio (VARIACION_VENTANA usa porcentaje y ventanaMinutos).
ALTER TABLE alertas ALTER COLUMN valor DROP NOT NULL
@@
