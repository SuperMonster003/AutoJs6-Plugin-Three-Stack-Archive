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

> La integración nativa requiere AutoJs6 6.8.0, código de versión 5276 o posterior, con Explorer Action v11. RAR es deliberadamente de solo lectura. La edición se limita a `.zip` ordinarios de un solo volumen. Los conjuntos ZIP/RAR divididos existentes aún no pueden leerse como un conjunto completo porque el host solo concede el descriptor del archivo seleccionado; el primer volumen RAR puede mostrar metadatos, pero la extracción permanece deshabilitada. El cifrado de nombres al crear, la eliminación de fuentes y la edición interna de 7Z, RAR o TAR no están disponibles.

### Uso

1. Instala Archive Manager y actívalo en el Centro de plugins de AutoJs6.
2. Pulsa la acción principal de un archivo compatible o elige Abrir archivo, y navega como por una carpeta con la barra de ruta del host.
3. Usa la acción de la barra de ruta para la carpeta interna actual, mantén pulsado para extraer una selección o elige Extraer en... en el menú del archivo para extraerlo completo. La contraseña se solicita cuando hace falta.
4. Elige Comprimir... para un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa la acción de la barra inferior.
5. Elige Gestionar archivo... solo para añadir, renombrar o eliminar contenido de un ZIP ordinario de un solo volumen.

### Permisos y datos

Archive Manager no solicita permisos de almacenamiento ni de red. El host entrega un descriptor de solo lectura de corta duración y transacciones de salida vinculadas al UID del plugin, por lo que este no puede elegir rutas arbitrarias. Explorer Action v11 transporta la contraseña solo en una solicitud síncrona y acotada; host y plugin eliminan y borran inmediatamente los búferes retenidos, sin persistirlos en estado, preferencias, registros o diagnósticos. Android y las bibliotecas Java aún pueden crear copias breves inevitables durante la ejecución: es higiene de memoria de mejor esfuerzo, no una garantía absoluta. Las rutas peligrosas siguen aisladas, la salida se verifica antes de publicarse y confirmar un presupuesto nunca desactiva la seguridad estructural.

### Roadmap

El trabajo restante se sigue con casillas verificables: acceso a volúmenes hermanos ZIP/RAR, corrección nativa de codificación de nombres, reconstrucciones editables más allá de ZIP, deshacer o eliminar fuentes mediante transacciones, accesibilidad y el resto de la matriz de dispositivos y productores.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### v2.4.0

_2026/08/26_

- `Nota` Esta versión requiere AutoJs6 6.8.0 con Explorer Action v11, código de versión 5276 o posterior
- `Añadido` Los archivos RAR4/RAR5 ya se pueden explorar, previsualizar y extraer, incluido el contenido o los encabezados cifrados; RAR sigue siendo deliberadamente de solo lectura
- `Añadido` La página nativa de AutoJs6 puede pedir una contraseña al abrir por primera vez o durante la extracción y reintentar sin perder la ruta ni la selección
- `Corregido` El primer volumen de un RAR dividido conserva sus metadatos legibles, pero deja de ofrecer extracción si no están disponibles los volúmenes hermanos
- `Corregido` Una contraseña errónea borra la entrada anterior y reintenta sobre una instantánea sin cambios sin salir de la página nativa
- `Mejorado` RAR lee directamente el descriptor seekable del host cuando es posible, no añade ABI nativas y reutiliza las comprobaciones de seguridad comunes
- `Dependencia` Se añadieron Junrar 8.1.0 y SLF4J 2.0.17 para RAR de solo lectura bajo sus licencias incluidas

#### v2.3.0

_2026/08/26_

- `Nota` Esta versión requiere la compilación asociada de AutoJs6 6.8.0 con Explorer Action v10 (código de versión 5276 o posterior)
- `Añadido` La página nativa de archivos ahora puede extraer la carpeta interna actual desde la barra de ruta o las entradas marcadas desde la barra de selección sin abrir una página de gestión separada
- `Añadido` La extracción nativa escribe mediante un árbol de salida propiedad del host, con progreso, cancelación, numeración segura de conflictos y actualización automática de Explorer
- `Corregido` Salir de un archivo en Android 7 ya no provoca un fallo mientras el host limpia la caché de vistas previas
- `Corregido` Las etiquetas de la barra de selección de cinco acciones se centran bajo sus iconos en pantallas estrechas
- `Mejorado` El modo de selección de archivos ahora solo muestra Salir y Extraer, y oculta las acciones del sistema de archivos que no se aplican dentro de un archivo

#### v2.2.0

_2026/08/26_

- `Nota` Esta versión requiere la compilación correspondiente de AutoJs6 6.8.0 con Explorer Action v9 (código de versión 5276 o posterior)
- `Añadido` Extraer en... ahora recomienda la carpeta actual y crea una carpeta de salida con el mismo nombre mediante una transacción de directorio propiedad del host; si ya existe un nombre equivalente, se numera de forma segura sin modificar su contenido
- `Añadido` La gestión de ZIP normales de un solo volumen puede importar una carpeta completa mediante el selector del sistema Android, incluidos los archivos anidados y las carpetas vacías
- `Corregido` El selector de destino de extracción permanece totalmente utilizable en pantallas estrechas y de poca altura y muestra la ruta predeterminada exacta
- `Corregido` Las extracciones en la misma carpeta canceladas, fallidas, interrumpidas o sin espacio revierten la salida no publicada; la siguiente sesión recupera las transacciones interrumpidas del host sin cambiar el archivo de origen
- `Mejorado` Explorer Action v9 publica atómicamente árboles de directorios verificados y actualiza la nueva carpeta de salida en AutoJs6 inmediatamente después de confirmarla

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
