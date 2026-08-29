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
- Cuando el nombre no coincide con un archivo compatible, ofrecer la comprobación explícita Abrir como archivo comprimido... y mostrar el formato detectado en la barra de ruta; los sufijos TAR compuestos exactos ya no hacen que flujos `.gz`, `.xz`, `.bz2` o `.zst` ordinarios parezcan archivos TAR.
- Previsualizar documentos, imágenes, audio y vídeo legibles con los visores existentes del host, sin extraer primero todo el archivo.
- Extraer el archivo completo, la carpeta interna actual o una selección con progreso, cancelación, nombres de conflicto seguros, verificación y reversión antes de publicar.
- Abrir y extraer ZIP, 7Z y RAR cifrados con una solicitud de contraseña nativa; una contraseña errónea puede corregirse sin perder la ruta actual.
- Corregir la codificación de nombres ZIP directamente desde la barra de ruta; la misma sesión de solo lectura reconstruye su índice y conserva cuando es posible la ruta interna y la selección disponibles.
- Explorar, previsualizar y extraer conjuntos completos estándar `.z01 + .zip`, WinRAR modernos `partN.rar`, `.zip.001` numerados y `.7z.001` numerados mediante descriptores hermanos acotados y autorizados por el host; los volúmenes ausentes o modificados fallan de forma explícita.
- Crear ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST desde un elemento o una selección con el mismo padre; ZIP también admite AES-256, volúmenes estándar y un archivo por elemento.
- Los ZIP divididos estándar y la compresión por elemento publican todas las salidas físicas verificadas mediante un lote recuperable; un fallo o reinicio del host nunca se presenta como un resultado parcial correcto.
- Editar ZIP ordinarios de un solo volumen y TAR, TAR.GZ/TGZ, TAR.XZ/TXZ o TAR.BZ2/TBZ2 compatibles mediante reconstrucción verificada: añadir archivos o árboles de carpetas, crear carpetas vacías, renombrar y eliminar, y sustituir la fuente de forma atómica.
- Tras editar correctamente un ZIP o un TAR editable, restaura la versión anterior desde el mensaje de éxito o el menú de gestión; el host ofrece una sola restauración durante una retención limitada y la rechaza si otra aplicación cambió el destino.
- La página de gestión reúne el formato real, los totales de contenido, las modificaciones disponibles y el motivo exacto del modo de solo lectura; antes de reservar una salida, cada cambio de ZIP o TAR editable muestra el trabajo real, la reconstrucción completa y los efectos en metadatos, mientras que cancelar no crea ninguna salida pendiente.
- Aislar en modo de solo lectura los nombres peligrosos, aplicar límites estructurales y de recursos antes de escribir y leer directamente el descriptor seekable del host cuando sea posible.
- Mover opcionalmente toda la selección de origen a la papelera del host solo después de verificar y confirmar todas las salidas físicas; la opción está desactivada de forma predeterminada y una fuente modificada o una prueba incompleta detiene la operación antes de retirar datos de origen.

### Formatos actuales

La versión actual reconoce estas extensiones explorables y extraíbles:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La versión actual puede crear estos formatos:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> La integración nativa requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v18 (código de versión 5276 o posterior). RAR, los archivos divididos y TAR.ZST/TZST son deliberadamente de solo lectura; se pueden editar `.zip`, `.tar`, `.tar.gz`, `.tgz`, `.tar.xz`, `.txz`, `.tar.bz2` y `.tbz2` ordinarios de un solo volumen que contengan solo archivos y directorios normales seguros. Abre un ZIP dividido estándar desde su `.zip` final, un conjunto WinRAR moderno desde su primer `partN.rar` y un ZIP o 7Z numerado desde su volumen `.001`, con todos los volúmenes necesarios en el mismo directorio. El cifrado de nombres al crear y la edición interna de JAR/AAR/WAR, 7Z, RAR o los demás contenedores TAR comprimidos siguen sin estar disponibles.

### Uso

1. Instala Archive Manager y actívalo en el Centro de plugins de AutoJs6.
2. Pulsa la acción principal de un archivo compatible o elige Abrir archivo. Si no se reconoce el nombre, elige Abrir como archivo comprimido... en el menú. Navega como por una carpeta con la barra de ruta, que indica el formato detectado cuando el nombre era engañoso.
3. Usa la acción de extracción de la barra de ruta para la carpeta interna actual o la acción de codificación para corregir nombres ZIP, mantén pulsado para extraer una selección o elige Extraer en... en el menú del archivo para extraerlo completo. La contraseña se solicita cuando hace falta.
4. Elige Comprimir... para un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa la acción de la barra inferior. Para limpiar las fuentes después del éxito, activa expresamente la opción desactivada de forma predeterminada que las mueve a la papelera tras comprimir.
5. Elige Gestionar archivo... para añadir, renombrar o eliminar contenido de un ZIP ordinario de un solo volumen o un TAR, TAR.GZ/TGZ, TAR.XZ/TXZ o TAR.BZ2/TBZ2 compatible.

### Permisos y datos

Archive Manager no solicita permisos de almacenamiento ni de red. El host entrega descriptores de solo lectura de corta duración y transacciones vinculadas al UID del plugin, por lo que este no puede elegir rutas arbitrarias. Explorer Action v11 transporta la contraseña solo en una solicitud síncrona acotada y nunca la persiste. Explorer Action v12 añade únicamente un catálogo acotado y ligado a la sesión de volúmenes hermanos aprobados, con ID opacos y revalidación de identidad. Explorer Action v13 solo reindexa la misma fuente preparada y conserva el estado anterior hasta que el índice completo está listo. Explorer Action v14 solo coincide con sufijos compuestos acotados como `.zip.001` y `.7z.001`, nunca con cualquier archivo `.001`, y reutiliza el catálogo v12 sin conceder acceso a rutas o directorios. Explorer Action v17 solo añade una acción secundaria de solo lectura y sin coincidencias cuando no coincide la acción principal; reutiliza una sesión existente tras la elección del usuario y no concede acceso nuevo a rutas, directorios ni escritura. Las rutas peligrosas siguen aisladas, la salida se verifica antes de publicarse y confirmar un presupuesto nunca desactiva la seguridad estructural.

Explorer Action v15 agrupa solo salidas nuevas verificadas de una sesión en un lote recuperable de hasta 128 miembros. Explorer Action v16 permite que el host revalide fuentes y salidas y mueva las fuentes a la papelera solo cuando el plugin aporta la selección original completa y ordenada y todas las transacciones de salida confirmadas. El host sincroniza una copia recuperable y persiste su registro antes de retirar datos de origen; el plugin no recibe rutas arbitrarias ni eliminación directa. Si se pierde una respuesta Binder se consulta el mismo resultado terminal idempotente, sin repetir el movimiento.

Explorer Action v18 guarda el archivo anterior solo en almacenamiento privado y persistente del host y devuelve un ID opaco, nunca una ruta de copia. Solo permite una restauración mientras el directorio padre y el destino coincidan exactamente con el reemplazo confirmado. Los cambios externos invalidan el historial; la evidencia de una recuperación interrumpida se conserva y bloquea otro reemplazo del mismo destino hasta que el host lo resuelva. El historial normal está limitado por antigüedad, cantidad, bytes totales y reserva de espacio libre; las sesiones v8-v17 no crean copias de reemplazo.

### Roadmap

El trabajo restante se sigue con casillas verificables: reconstrucciones editables de TAR.ZST y 7Z, deshacer en grupo e historial de la papelera, el resto de la matriz de dispositivos y productores y el material para la primera publicación pública.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### v2.15.0

_2026/08/29_

- `Nota` Esta versión sigue requiriendo la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v18 (código de versión 5276 o posterior)
- `Añadido` Los archivos TAR.BZ2 y TBZ2 que solo contienen archivos y directorios normales seguros ya permiten añadir archivos o árboles completos, crear carpetas vacías, cambiar nombres y eliminar desde la página de gestión
- `Corregido` La acción de gestión ahora reconoce `.tar.bz2` y `.tbz2` con precisión; TAR.ZST/TZST es el único contenedor TAR comprimido que sigue siendo de solo lectura
- `Mejorado` Los cambios de TAR.BZ2 usan el preset 6 acotado de tamaño de bloque BZIP2 y se reconstruyen directamente en una salida pendiente del host; la cancelación, la integridad del final, los fallos reales de escritura, los cambios de origen y la lectura completa permanecen dentro del límite de reversión

#### v2.14.0

_2026/08/29_

- `Nota` Esta versión sigue requiriendo la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v18 (código de versión 5276 o posterior)
- `Añadido` Los archivos TAR.XZ y TXZ que solo contienen archivos y directorios normales seguros ya permiten añadir archivos o árboles completos, crear carpetas vacías, cambiar nombres y eliminar desde la página de gestión
- `Corregido` La acción de gestión ahora reconoce `.tar.xz` y `.txz` con precisión, mientras TAR.BZ2/TBZ2 y TAR.ZST/TZST siguen siendo de solo lectura
- `Mejorado` Los cambios de TAR.XZ usan el preset XZ 4 fijo (diccionario de 4 MiB; 48.058 KiB de memoria de codificación indicada por la biblioteca, por debajo de un presupuesto de 64 MiB) y se reconstruyen directamente en una salida pendiente del host; la cancelación, la integridad del final del contenedor, los fallos reales de escritura, los cambios de origen y la lectura completa permanecen dentro del límite de reversión

#### v2.13.0

_2026/08/29_

- `Nota` Esta versión sigue requiriendo la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v18 (código de versión 5276 o posterior)
- `Añadido` Los TAR.GZ y TGZ que solo contienen archivos y directorios normales seguros ya pueden añadir archivos o árboles completos, crear carpetas vacías, renombrar y eliminar desde la página de gestión
- `Corregido` La acción de gestión reconoce con precisión `.tar.gz` y `.tgz`, pero sigue rechazando JAR/AAR/WAR y los contenedores TAR.XZ, TAR.BZ2 y TAR.ZST de solo lectura
- `Mejorado` Los cambios TAR.GZ reconstruyen el flujo fuente comprimido directamente en la salida pendiente del host, sin una copia TAR privada sin comprimir; la cancelación, la integridad del final GZIP, los fallos de escritura y la relectura completa permanecen dentro del límite de reversión

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
