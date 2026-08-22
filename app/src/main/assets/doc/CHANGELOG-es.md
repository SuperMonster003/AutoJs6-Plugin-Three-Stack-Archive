# Notas de la versión

## Unreleased

_Sin publicar_

- `Añadido` El producto pasa a llamarse Archive Manager y la acción, Abrir archivo comprimido
- `Añadido` Explorer Action v5 explora archivos en la lista nativa de AutoJs6 con la barra de ruta, el tema y la navegación Atrás existentes
- `Añadido` Explorer Action v6 abre entradas compatibles con los visores de documentos, imágenes, audio y vídeo del host
- `Añadido` Acceso Extraer en... para elegir destino y extraer todo el archivo
- `Añadido` Explorer Action v4 añade Comprimir... a los menús de archivos y carpetas y a la barra de cinco acciones para selecciones del mismo directorio padre
- `Añadido` Creación de ZIP con nombre predeterminado, niveles de compresión, progreso, cancelación y numeración automática de conflictos
- `Añadido` Exploración y extracción de ZIP cifrados con ZipCrypto/AES, reintento de una contraseña incorrecta en el mismo lugar y creación opcional de ZIP con AES-256 cuyos nombres siguen visibles y confirmación de contraseña coincidente
- `Corregido` La lista ZIP usa metadatos y admite preámbulos autoextraíbles, codificaciones antiguas, separadores Windows y más métodos legibles
- `Corregido` La codificación de nombres ZIP se puede cambiar cuando la detección automática sea incorrecta y la extracción reutiliza la selección
- `Corregido` Los tamaños desconocidos, URI DocumentsProvider válidos y permisos de escritura adicionales del host ya no bloquean archivos válidos
- `Corregido` Corregida la exploración y extracción de ZIP en Android 7.x, que fallaba al llamar a API exclusivas de sistemas más recientes
- `Corregido` Las contraseñas incorrectas ahora se clasifican de forma estable como PASSWORD/WRONG_PASSWORD y las entradas AES v2 con CRC almacenado igual a cero ya no se marcan erróneamente como dañadas
- `Mejorado` Se eliminaron el límite fijo de 4 GiB y los umbrales de tamaño/ratio al explorar, manteniendo aislamiento y verificaciones
- `Mejorado` Se añadió un Roadmap verificable y se reescribieron README y CHANGELOG
- `Mejorado` La pantalla independiente sigue ahora el modo día/noche y los colores dinámicos Material
- `Mejorado` La salida ZIP pasa por una sesión del host vinculada al UID, usa un archivo temporal del mismo directorio y se confirma atómicamente sin permiso de almacenamiento ni sobrescrituras
- `Mejorado` Las capacidades del formato y de cada entrada se comprueban de forma uniforme al previsualizar, extraer y crear, por lo que las opciones no disponibles permanecen desactivadas
- `Mejorado` Los fallos identifican el formato, la etapa, un código estable y el motivo; las compilaciones de depuración permiten copiar el diagnóstico completo
- `Dependencia` Se añadió Zip4j 2.11.5 con licencia Apache 2.0 para flujos ZIP cifrados, creación AES-256 y la ruta de compatibilidad con Android 7.x

## v1.0.1

_2026/08/08_

- `Corregido` Enlace de servicio vacío al activar el complemento
- `Mejorado` Nombre, descripción e instrucciones más simples

## v1.0.0

_2026/08/02_

- `Añadido` Primera versión para explorar ZIP, JAR, AAR y WAR y extraer la selección
- `Añadido` Búsqueda, selección, progreso, cancelación, limpieza temporal e interfaz localizada
