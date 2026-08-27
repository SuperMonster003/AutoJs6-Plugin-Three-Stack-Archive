# Notas de la versión

## v2.8.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v15 (código de versión 5276 o posterior)
- `Añadido` Los ZIP divididos estándar y Comprimir cada elemento por separado ahora terminan la escritura y la verificación de lectura de todas las salidas antes de una publicación por lote recuperable de Explorer Action v15
- `Corregido` La creación de varias salidas ya no deja resultados parciales confirmados en fallos normales; Explorer se actualiza y se informa del éxito solo después de confirmar el lote completo
- `Corregido` Los interruptores de las opciones de compresión ahora se dibujan correctamente y se pueden pulsar en Android 7, en lugar de aparecer solo como etiquetas
- `Mejorado` El host registra de forma duradera el directorio padre y la identidad de cada archivo temporal antes de publicar; un fallo o reinicio revierte solo los miembros coincidentes y conserva los archivos modificados externamente para recuperación manual

## v2.7.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v14 (código de versión 5276 o posterior)
- `Añadido` Los conjuntos numerados completos `.zip.001` y `.7z.001` ya se pueden explorar, previsualizar y extraer abriendo su volumen `.001`; permanecen de solo lectura
- `Añadido` Explorer Action v14 añade coincidencia acotada de sufijos compuestos y reutiliza la fuente de volúmenes hermanos v12 vinculada al UID, sin coincidir con archivos `.001` arbitrarios
- `Corregido` Android 7 combina los volúmenes ZIP numerados autorizados por el host en un archivo local privado antes de usar la ruta compatible con Zip4j, por lo que los conjuntos válidos ya no se notifican como dañados
- `Mejorado` Los números de volúmenes hermanos se limitan de `.002` a `.128` y todos los volúmenes presentes deben ser contiguos; el lector informa el siguiente ausente, revalida la identidad al materializar y nunca anuncia modificación interna

## v2.6.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v13 (código de versión 5276 o posterior)
- `Añadido` La página nativa del archivo ahora permite elegir la codificación de nombres ZIP desde la barra de ruta sin abrir la página de gestión; el cambio conserva la carpeta interna y los elementos seleccionados que aún existen
- `Añadido` Explorer Action v13 reindexa la fuente almacenada en la misma sesión de solo lectura y usa ID estables para restaurar la ruta disponible más profunda y los elementos que aún existen
- `Mejorado` El índice de reemplazo se publica solo cuando está completo; una opción inválida, un fallo de análisis, una vista previa o una extracción activa conserva el índice anterior, y la contraseña permanece solo en memoria borrable

## v2.5.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v12 (código de versión 5276 o posterior)
- `Añadido` Los conjuntos completos estándar `.z01 + .zip` y los conjuntos modernos de WinRAR `partN.rar` ya se pueden explorar, previsualizar y extraer en la página nativa; los archivos divididos siguen siendo de solo lectura
- `Añadido` Explorer Action v12 entrega solo un catálogo acotado de volúmenes hermanos aprobados por el host y abre cada uno mediante un ID opaco como descriptor de solo lectura, sin exponer directorios ni rutas del sistema
- `Corregido` Los metadatos del directorio del volumen ZIP final se aceptan correctamente en Android 7 y posteriores, y los datos se leen en todos los volúmenes autorizados sin considerar dañado el volumen final
- `Corregido` Los CRC de segmentos RAR ya no se comparan con los datos reconstruidos; los volúmenes ausentes o modificados después de abrir producen errores tipados estables
- `Mejorado` La cantidad, los nombres, los ID, las aperturas, el UID llamante, la identidad de archivo y la vida de sesión están acotados y se revalidan; una copia interrumpida elimina todos los fragmentos de caché privada

## v2.4.0

_2026/08/26_

- `Nota` Esta versión requiere AutoJs6 6.8.0 con Explorer Action v11, código de versión 5276 o posterior
- `Añadido` Los archivos RAR4/RAR5 ya se pueden explorar, previsualizar y extraer, incluido el contenido o los encabezados cifrados; RAR sigue siendo deliberadamente de solo lectura
- `Añadido` La página nativa de AutoJs6 puede pedir una contraseña al abrir por primera vez o durante la extracción y reintentar sin perder la ruta ni la selección
- `Corregido` El primer volumen de un RAR dividido conserva sus metadatos legibles, pero deja de ofrecer extracción si no están disponibles los volúmenes hermanos
- `Corregido` Una contraseña errónea borra la entrada anterior y reintenta sobre una instantánea sin cambios sin salir de la página nativa
- `Mejorado` RAR lee directamente el descriptor seekable del host cuando es posible, no añade ABI nativas y reutiliza las comprobaciones de seguridad comunes
- `Dependencia` Se añadieron Junrar 8.1.0 y SLF4J 2.0.17 para RAR de solo lectura bajo sus licencias incluidas

## v2.3.0

_2026/08/26_

- `Nota` Esta versión requiere la compilación asociada de AutoJs6 6.8.0 con Explorer Action v10 (código de versión 5276 o posterior)
- `Añadido` La página nativa de archivos ahora puede extraer la carpeta interna actual desde la barra de ruta o las entradas marcadas desde la barra de selección sin abrir una página de gestión separada
- `Añadido` La extracción nativa escribe mediante un árbol de salida propiedad del host, con progreso, cancelación, numeración segura de conflictos y actualización automática de Explorer
- `Corregido` Salir de un archivo en Android 7 ya no provoca un fallo mientras el host limpia la caché de vistas previas
- `Corregido` Las etiquetas de la barra de selección de cinco acciones se centran bajo sus iconos en pantallas estrechas
- `Mejorado` El modo de selección de archivos ahora solo muestra Salir y Extraer, y oculta las acciones del sistema de archivos que no se aplican dentro de un archivo

## v2.2.0

_2026/08/26_

- `Nota` Esta versión requiere la compilación correspondiente de AutoJs6 6.8.0 con Explorer Action v9 (código de versión 5276 o posterior)
- `Añadido` Extraer en... ahora recomienda la carpeta actual y crea una carpeta de salida con el mismo nombre mediante una transacción de directorio propiedad del host; si ya existe un nombre equivalente, se numera de forma segura sin modificar su contenido
- `Añadido` La gestión de ZIP normales de un solo volumen puede importar una carpeta completa mediante el selector del sistema Android, incluidos los archivos anidados y las carpetas vacías
- `Corregido` El selector de destino de extracción permanece totalmente utilizable en pantallas estrechas y de poca altura y muestra la ruta predeterminada exacta
- `Corregido` Las extracciones en la misma carpeta canceladas, fallidas, interrumpidas o sin espacio revierten la salida no publicada; la siguiente sesión recupera las transacciones interrumpidas del host sin cambiar el archivo de origen
- `Mejorado` Explorer Action v9 publica atómicamente árboles de directorios verificados y actualiza la nueva carpeta de salida en AutoJs6 inmediatamente después de confirmarla

## v2.1.0

_2026/08/25_

- `Nota` La edición se limita actualmente a archivos `.zip` normales de un solo volumen. JAR/AAR/WAR, ZIP dividido, 7Z y la familia TAR siguen siendo de solo lectura; la reconstrucción normaliza los comentarios, los metadatos extra no esenciales y los atributos de permisos Unix
- `Añadido` Administrar archivo... abre el ZIP seleccionado en la página de gestión con Añadir archivos..., Nueva carpeta..., Renombrar... y Eliminar, incluido el cambio de nombre y la eliminación de subárboles de directorios
- `Añadido` Explorer Action v8 reconstruye en una salida pendiente propiedad del host, relee por completo el resultado, reemplaza atómicamente el original solo tras verificarlo y actualiza automáticamente la fila de Explorer
- `Mejorado` Cada cambio se valida antes como un plan inmutable que comprueba rutas peligrosas, nombres duplicados o equivalentes, conflictos de archivo/directorio, entradas conservadas no compatibles y cambios de la fuente antes de confirmar la salida de reemplazo
- `Mejorado` La reconstrucción ZIP conserva el contenido Stored/Deflate, las marcas de tiempo utilizables y el cifrado ZipCrypto/AES compatible; la cancelación o cualquier fallo de validación descarta la salida pendiente y deja intacto el archivo original

## v2.0.0

_2026/08/25_

- `Añadido` El producto pasa a llamarse Archive Manager y la acción, Abrir archivo comprimido
- `Añadido` Explorer Action v5 explora archivos en la lista nativa de AutoJs6 con la barra de ruta, el tema y la navegación Atrás existentes
- `Añadido` Explorer Action v6 abre entradas compatibles con los visores de documentos, imágenes, audio y vídeo del host
- `Añadido` Acceso Extraer en... para elegir destino y extraer todo el archivo
- `Añadido` Gestionar archivo... abre la página de gestión con los ámbitos de extracción de archivo completo, carpeta interna actual y selección marcada, manteniendo el acceso directo para todo el archivo
- `Añadido` Políticas de conflicto al extraer para preguntar, omitir, sobrescribir o renombrar automáticamente, con aplicación global, recuentos precisos y conservación de carpetas de salida existentes
- `Añadido` Explorer Action v4 añade Comprimir... a los menús de archivos y carpetas y a la barra de cinco acciones para selecciones del mismo directorio padre
- `Añadido` Creación de ZIP con nombre predeterminado, niveles de compresión, progreso, cancelación y numeración automática de conflictos
- `Añadido` Creación de ZIP divididos estándar, también con AES-256, mediante tamaños MiB habituales o un entero personalizado; todo el conjunto comparte un nombre base seguro y el `.zip` final aparece después de los volúmenes numerados
- `Añadido` Creación de un archivo por elemento de una selección del mismo directorio padre, con vista previa de salidas, numeración automática de conflictos y conservación explícita de las salidas completadas tras un fallo o una cancelación posteriores
- `Añadido` Creación de 7Z no solid con niveles de 0 a 9 y cifrado de contenido AES-256 opcional; los nombres siguen visibles y no se anuncia cifrado de nombres inexistente
- `Añadido` Creación de TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST con niveles propios y actualización completa de extensiones compuestas
- `Añadido` Exploración y extracción de ZIP cifrados con ZipCrypto/AES, reintento de una contraseña incorrecta en el mismo lugar y creación opcional de ZIP con AES-256 cuyos nombres siguen visibles y confirmación de contraseña coincidente
- `Añadido` Exploración, vista previa y extracción de 7Z normales o solid con cadenas comunes de compresión y filtros, cifrado AES del contenido y de la cabecera; las contraseñas ausentes o erróneas reciben un diagnóstico explícito
- `Añadido` Exploración, vista previa y extracción de TAR sin comprimir en la lista nativa con validación de la suma de comprobación de cabeceras; enlaces, nodos de dispositivo y entradas dispersas quedan solo para lectura
- `Añadido` Exploración, vista previa y extracción de TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 y TAR.ZST/TZST por las mismas rutas nativas; la detección verifica la firma del compresor y la estructura TAR interna
- `Añadido` Presupuestos de extracción Compatible, Estricto y Personalizado; los archivos que superan el presupuesto siguen disponibles en modo de solo lectura y muestran salida estimada, dimensiones excedidas y una confirmación única antes de escribir
- `Añadido` El progreso de extracción muestra elementos, bytes, el elemento actual, la velocidad y el tiempo restante estimado, con cancelación fiable
- `Corregido` La lista ZIP usa metadatos y admite preámbulos autoextraíbles, codificaciones antiguas, separadores Windows y más métodos legibles
- `Corregido` Los ZIP divididos estándar `.z01 + .zip` ahora indican los volúmenes anteriores necesarios en lugar de marcar el volumen final como dañado
- `Corregido` La codificación de nombres ZIP se puede cambiar cuando la detección automática sea incorrecta y la extracción reutiliza la selección
- `Corregido` Los tamaños desconocidos, URI DocumentsProvider válidos y permisos de escritura adicionales del host ya no bloquean archivos válidos
- `Corregido` Corregida la exploración y extracción de ZIP en Android 7.x, que fallaba al llamar a API exclusivas de sistemas más recientes
- `Corregido` Los ZIP con nombres Unicode se abren ahora correctamente en Android 7, incluso si falta la marca de nombre UTF-8 o el backend la interpreta de forma incoherente
- `Corregido` Las contraseñas incorrectas ahora se clasifican de forma estable como PASSWORD/WRONG_PASSWORD y las entradas AES v2 con CRC almacenado igual a cero ya no se marcan erróneamente como dañadas
- `Corregido` La carpeta de extracción predeterminada de extensiones compuestas como TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST elimina ahora el sufijo completo en lugar de conservar `.tar`
- `Corregido` Los archivos con nombres que contienen recorridos al directorio padre, rutas absolutas, prefijos de unidad o caracteres de control siguen siendo explorables; esos nombres pasan a una carpeta aislada de solo lectura, conservan la vista previa cuando sus datos son legibles y deben omitirse explícitamente antes de extraer
- `Corregido` La extracción ya no sobrescribe archivos ni carpetas existentes cuando el proveedor de destino considera idénticos los nombres que solo difieren en mayúsculas o minúsculas o son equivalentes en Unicode; las carpetas de salida equivalentes se numeran automáticamente
- `Corregido` La cancelación o un fallo de extracción revierte la nueva raíz de salida en una fase de limpieza no cancelable; si el proveedor rechaza la eliminación, se muestran el nombre y el URI del posible residuo en lugar de solo un error genérico
- `Corregido` La creación de 7Z cifrados funciona ahora en Android 7 y la verificación empareja entradas por ruta para que las diferencias válidas de orden del backend no produzcan fallos falsos
- `Corregido` La página de gestión se adapta ahora a pantallas bajas en vertical y horizontal: el archivo y la ruta pasan a la barra, los ajustes siguen disponibles en una fila horizontal compacta y las entradas y acciones permanecen visibles con fuentes de hasta 2,0x
- `Mejorado` Se eliminaron el límite fijo de 4 GiB y los umbrales de tamaño/ratio al explorar, manteniendo aislamiento y verificaciones
- `Mejorado` Se añadió un Roadmap verificable y se reescribieron README y CHANGELOG
- `Mejorado` La pantalla independiente sigue ahora el modo día/noche y los colores dinámicos Material
- `Mejorado` Las salidas ZIP, 7Z y TAR pasan por una sesión del host vinculada al UID, usan un archivo temporal del mismo directorio y se confirman atómicamente sin permiso de almacenamiento ni sobrescrituras
- `Mejorado` Las capacidades del formato y de cada entrada se comprueban de forma uniforme al previsualizar, extraer y crear, por lo que las opciones no disponibles permanecen desactivadas
- `Mejorado` Los fallos identifican el formato, la etapa, un código estable y el motivo; las compilaciones de depuración permiten copiar el diagnóstico completo
- `Mejorado` Los archivos normales ahora se exploran mediante canales con posición independiente sobre el descriptor de solo lectura del host, sin copiar todo el archivo; las entradas o lectores incompatibles (actualmente incluido ZIP cifrado) recurren a la caché privada, que se limpia al cerrar, fallar o caducar
- `Mejorado` Se unificaron las transacciones de salida al crear ZIP, 7Z y TAR; las fuentes ilegibles y los fallos al reservar, abrir, escribir o confirmar tienen etapas estables, mientras que una cancelación no confirmada cierra la sesión, muestra la ruta prevista e impide un reintento inseguro
- `Mejorado` La creación analiza el origen antes de abrir la salida temporal y muestra estados separados de análisis, compresión, verificación y confirmación, con el total de archivos, los bytes leídos y los tamaños desconocidos
- `Mejorado` Los archivos creados se vuelven a leer por completo antes de publicarlos para comprobar formato, entradas, tamaños, CRC y huellas del contenido; cada volumen ZIP pendiente también se compara byte a byte
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
