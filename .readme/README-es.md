<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Un gestor de archivos comprimidos integrado en AutoJs6 para explorar, extraer, crear y editar de forma segura los formatos compatibles</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### Idiomas (Languages)

El README está disponible en los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### Acerca del proyecto

Archive Manager funciona dentro del gestor de archivos de AutoJs6 en lugar de sustituirlo. Los archivos compatibles usan la lista, la barra de ruta, el tema, los visores, la selección, el progreso y la actualización del host. La página de gestión separada queda para ajustes y operaciones que necesitan un formulario más completo.

### Disponible ahora

- Explorar ZIP/JAR/AAR/WAR, 7Z, RAR4/RAR5 y la familia TAR en la lista nativa de AutoJs6, con ruta interna, búsqueda, ordenación y navegación Atrás.
- Previsualizar documentos, imágenes, audio y vídeo legibles con los visores existentes del host, sin extraer primero todo el archivo.
- Extraer el archivo completo, la carpeta interna actual o una selección con progreso, cancelación, nombres de conflicto seguros, verificación y reversión antes de publicar.
- Abrir y extraer ZIP, 7Z y RAR cifrados con una solicitud de contraseña nativa; una contraseña errónea puede corregirse sin perder la ruta actual.
- Corregir la codificación de nombres ZIP directamente desde la barra de ruta; la misma sesión de solo lectura reconstruye su índice y conserva cuando es posible la ruta interna y la selección disponibles.
- Explorar, previsualizar y extraer conjuntos completos estándar `.z01 + .zip` y conjuntos modernos WinRAR `partN.rar` mediante descriptores hermanos acotados y autorizados por el host; los volúmenes ausentes o modificados fallan de forma explícita.
- Crear ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST desde un elemento o una selección con el mismo padre; ZIP también admite AES-256, volúmenes estándar y un archivo por elemento.
- Editar un ZIP ordinario de un solo volumen mediante reconstrucción verificada: añadir archivos o árboles de carpetas, crear carpetas vacías, renombrar y eliminar, y sustituir la fuente de forma atómica.
- Aislar en modo de solo lectura los nombres peligrosos, aplicar límites estructurales y de recursos antes de escribir y leer directamente el descriptor seekable del host cuando sea posible.

### Formatos actuales

La versión actual reconoce estas extensiones explorables y extraíbles:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La versión actual puede crear estos formatos:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> La integración nativa requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v13 (código de versión 5276 o posterior). RAR y los archivos divididos son deliberadamente de solo lectura; la edición se limita a `.zip` ordinarios de un solo volumen. Abre un ZIP dividido estándar desde su `.zip` final o un conjunto WinRAR moderno desde su primer `partN.rar`, con todos los volúmenes necesarios en el mismo directorio. Las secuencias `.zip.001`, 7Z dividido, el cifrado de nombres al crear, la eliminación de fuentes y la edición interna de 7Z, RAR o TAR no están disponibles.

### Uso

1. Instala Archive Manager y actívalo en el Centro de plugins de AutoJs6.
2. Pulsa la acción principal de un archivo compatible o elige Abrir archivo, y navega como por una carpeta con la barra de ruta del host.
3. Usa la acción de extracción de la barra de ruta para la carpeta interna actual o la acción de codificación para corregir nombres ZIP, mantén pulsado para extraer una selección o elige Extraer en... en el menú del archivo para extraerlo completo. La contraseña se solicita cuando hace falta.
4. Elige Comprimir... para un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa la acción de la barra inferior.
5. Elige Gestionar archivo... solo para añadir, renombrar o eliminar contenido de un ZIP ordinario de un solo volumen.

### Permisos y datos

Archive Manager no solicita permisos de almacenamiento ni de red. El host entrega descriptores de solo lectura de corta duración y transacciones vinculadas al UID del plugin, por lo que este no puede elegir rutas arbitrarias. Explorer Action v11 transporta la contraseña solo en una solicitud síncrona acotada; ambos lados eliminan y borran inmediatamente sus búferes y nunca la persisten. Explorer Action v12 añade únicamente un catálogo acotado y ligado a la sesión de volúmenes hermanos aprobados: el plugin recibe ID opacos, no rutas, y se revalidan el UID llamante, la identidad, el tamaño, la fecha y el ciclo de vida. Explorer Action v13 solo reindexa la misma fuente preparada, limita la lista y los nombres de codificación y conserva el estado anterior hasta que el índice de reemplazo completo está listo. Las copias breves inevitables de Android o Java hacen que la limpieza de contraseñas sea de mejor esfuerzo. Las rutas peligrosas siguen aisladas, la salida se verifica antes de publicarse y confirmar un presupuesto nunca desactiva la seguridad estructural.

### Roadmap

El trabajo restante se sigue con casillas verificables: investigación de `.zip.001` y 7Z dividido, reconstrucciones editables más allá de ZIP, deshacer o eliminar fuentes mediante transacciones, accesibilidad y el resto de la matriz de dispositivos y productores.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### v2.6.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v13 (código de versión 5276 o posterior)
- `Añadido` La página nativa del archivo ahora permite elegir la codificación de nombres ZIP desde la barra de ruta sin abrir la página de gestión; el cambio conserva la carpeta interna y los elementos seleccionados que aún existen
- `Añadido` Explorer Action v13 reindexa la fuente almacenada en la misma sesión de solo lectura y usa ID estables para restaurar la ruta disponible más profunda y los elementos que aún existen
- `Mejorado` El índice de reemplazo se publica solo cuando está completo; una opción inválida, un fallo de análisis, una vista previa o una extracción activa conserva el índice anterior, y la contraseña permanece solo en memoria borrable

#### v2.5.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v12 (código de versión 5276 o posterior)
- `Añadido` Los conjuntos completos estándar `.z01 + .zip` y los conjuntos modernos de WinRAR `partN.rar` ya se pueden explorar, previsualizar y extraer en la página nativa; los archivos divididos siguen siendo de solo lectura
- `Añadido` Explorer Action v12 entrega solo un catálogo acotado de volúmenes hermanos aprobados por el host y abre cada uno mediante un ID opaco como descriptor de solo lectura, sin exponer directorios ni rutas del sistema
- `Corregido` Los metadatos del directorio del volumen ZIP final se aceptan correctamente en Android 7 y posteriores, y los datos se leen en todos los volúmenes autorizados sin considerar dañado el volumen final
- `Corregido` Los CRC de segmentos RAR ya no se comparan con los datos reconstruidos; los volúmenes ausentes o modificados después de abrir producen errores tipados estables
- `Mejorado` La cantidad, los nombres, los ID, las aperturas, el UID llamante, la identidad de archivo y la vida de sesión están acotados y se revalidan; una copia interrumpida elimina todos los fragmentos de caché privada

#### v2.4.0

_2026/08/26_

- `Nota` Esta versión requiere AutoJs6 6.8.0 con Explorer Action v11, código de versión 5276 o posterior
- `Añadido` Los archivos RAR4/RAR5 ya se pueden explorar, previsualizar y extraer, incluido el contenido o los encabezados cifrados; RAR sigue siendo deliberadamente de solo lectura
- `Añadido` La página nativa de AutoJs6 puede pedir una contraseña al abrir por primera vez o durante la extracción y reintentar sin perder la ruta ni la selección
- `Corregido` El primer volumen de un RAR dividido conserva sus metadatos legibles, pero deja de ofrecer extracción si no están disponibles los volúmenes hermanos
- `Corregido` Una contraseña errónea borra la entrada anterior y reintenta sobre una instantánea sin cambios sin salir de la página nativa
- `Mejorado` RAR lee directamente el descriptor seekable del host cuando es posible, no añade ABI nativas y reutiliza las comprobaciones de seguridad comunes
- `Dependencia` Se añadieron Junrar 8.1.0 y SLF4J 2.0.17 para RAR de solo lectura bajo sus licencias incluidas

##### Historial completo

* [CHANGELOG-es.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

### Compilación

```powershell
.\gradlew.bat :app:assembleDebug
```

Compilación Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Usa Gradle Wrapper desde la raíz; `version.properties` define los requisitos de SDK y JDK.

### Enlaces

- Documentación de AutoJs6: https://docs.autojs6.com
- Avisos de software de terceros: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
