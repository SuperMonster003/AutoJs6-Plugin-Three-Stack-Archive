# Notas de la versión

## Unreleased

_Sin publicar_

- `Añadido` El producto pasa a llamarse Archive Manager y la acción, Abrir archivo comprimido
- `Añadido` Explorer Action v5 explora archivos en la lista nativa de AutoJs6 con la barra de ruta, el tema y la navegación Atrás existentes
- `Añadido` Explorer Action v6 abre entradas compatibles con los visores de documentos, imágenes, audio y vídeo del host
- `Añadido` Acceso Extraer en... para elegir destino y extraer todo el archivo
- `Añadido` Explorer Action v4 añade Comprimir... a los menús de archivos y carpetas y a la barra de cinco acciones para selecciones del mismo directorio padre
- `Añadido` Creación de ZIP con nombre predeterminado, niveles de compresión, progreso, cancelación y numeración automática de conflictos
- `Añadido` Creación de 7Z no solid con niveles de 0 a 9 y cifrado de contenido AES-256 opcional; los nombres siguen visibles y no se anuncia cifrado de nombres inexistente
- `Añadido` Creación de TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST con niveles propios y actualización completa de extensiones compuestas
- `Añadido` Exploración y extracción de ZIP cifrados con ZipCrypto/AES, reintento de una contraseña incorrecta en el mismo lugar y creación opcional de ZIP con AES-256 cuyos nombres siguen visibles y confirmación de contraseña coincidente
- `Añadido` Exploración, vista previa y extracción de 7Z normales o solid con cadenas comunes de compresión y filtros, cifrado AES del contenido y de la cabecera; las contraseñas ausentes o erróneas reciben un diagnóstico explícito
- `Añadido` Exploración, vista previa y extracción de TAR sin comprimir en la lista nativa con validación de la suma de comprobación de cabeceras; enlaces, nodos de dispositivo y entradas dispersas quedan solo para lectura
- `Añadido` Exploración, vista previa y extracción de TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 y TAR.ZST/TZST por las mismas rutas nativas; la detección verifica la firma del compresor y la estructura TAR interna
- `Corregido` La lista ZIP usa metadatos y admite preámbulos autoextraíbles, codificaciones antiguas, separadores Windows y más métodos legibles
- `Corregido` Los ZIP divididos estándar `.z01 + .zip` ahora indican los volúmenes anteriores necesarios en lugar de marcar el volumen final como dañado
- `Corregido` Los archivos con nombres que contienen recorridos al directorio padre, rutas absolutas, prefijos de unidad o caracteres de control siguen siendo explorables; esos nombres pasan a una carpeta aislada de solo lectura, conservan la vista previa cuando sus datos son legibles y deben omitirse explícitamente antes de extraer
- `Corregido` La codificación de nombres ZIP se puede cambiar cuando la detección automática sea incorrecta y la extracción reutiliza la selección
- `Corregido` Los tamaños desconocidos, URI DocumentsProvider válidos y permisos de escritura adicionales del host ya no bloquean archivos válidos
- `Corregido` Corregida la exploración y extracción de ZIP en Android 7.x, que fallaba al llamar a API exclusivas de sistemas más recientes
- `Corregido` Las contraseñas incorrectas ahora se clasifican de forma estable como PASSWORD/WRONG_PASSWORD y las entradas AES v2 con CRC almacenado igual a cero ya no se marcan erróneamente como dañadas
- `Corregido` La carpeta de extracción predeterminada de extensiones compuestas como TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST elimina ahora el sufijo completo en lugar de conservar `.tar`
- `Mejorado` Se eliminaron el límite fijo de 4 GiB y los umbrales de tamaño/ratio al explorar, manteniendo aislamiento y verificaciones
- `Añadido` Presupuestos de extracción Compatible, Estricto y Personalizado; los archivos que superan el presupuesto siguen disponibles en modo de solo lectura y muestran salida estimada, dimensiones excedidas y una confirmación única antes de escribir
- `Mejorado` Se añadió un Roadmap verificable y se reescribieron README y CHANGELOG
- `Mejorado` La pantalla independiente sigue ahora el modo día/noche y los colores dinámicos Material
- `Mejorado` Las salidas ZIP, 7Z y TAR pasan por una sesión del host vinculada al UID, usan un archivo temporal del mismo directorio y se confirman atómicamente sin permiso de almacenamiento ni sobrescrituras
- `Mejorado` Las capacidades del formato y de cada entrada se comprueban de forma uniforme al previsualizar, extraer y crear, por lo que las opciones no disponibles permanecen desactivadas
- `Mejorado` Los fallos identifican el formato, la etapa, un código estable y el motivo; las compilaciones de depuración permiten copiar el diagnóstico completo
- `Dependencia` Se añadió Zip4j 2.11.5 con licencia Apache 2.0 para flujos ZIP cifrados, creación AES-256 y la ruta de compatibilidad con Android 7.x
- `Dependencia` Se añadió XZ for Java 1.12 con licencia 0BSD para leer y escribir TAR.XZ/TXZ en Java puro sin ABI nativas
- `Dependencia` Se añadió zstd-jni 1.5.7-15 con licencia BSD para leer y escribir TAR.ZST/TZST; las cuatro ABI de Android superan las comprobaciones de alineación ELF de 16 KiB y RELRO

## v1.0.1

_2026/08/08_

- `Corregido` Enlace de servicio vacío al activar el complemento
- `Mejorado` Nombre, descripción e instrucciones más simples

## v1.0.0

_2026/08/02_

- `Añadido` Primera versión para explorar ZIP, JAR, AAR y WAR y extraer la selección
- `Añadido` Búsqueda, selección, progreso, cancelación, limpieza temporal e interfaz localizada
