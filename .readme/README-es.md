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
- Explorar, previsualizar y extraer conjuntos completos estándar `.z01 + .zip`, WinRAR modernos `partN.rar`, `.zip.001` numerados y `.7z.001` numerados mediante descriptores hermanos acotados y autorizados por el host; los volúmenes ausentes o modificados fallan de forma explícita.
- Crear ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST desde un elemento o una selección con el mismo padre; ZIP también admite AES-256, volúmenes estándar y un archivo por elemento.
- Los ZIP divididos estándar y la compresión por elemento publican todas las salidas físicas verificadas mediante un lote recuperable; un fallo o reinicio del host nunca se presenta como un resultado parcial correcto.
- Editar un ZIP ordinario de un solo volumen mediante reconstrucción verificada: añadir archivos o árboles de carpetas, crear carpetas vacías, renombrar y eliminar, y sustituir la fuente de forma atómica.
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

> La integración nativa requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v16 (código de versión 5276 o posterior). RAR y los archivos divididos son deliberadamente de solo lectura; la edición se limita a `.zip` ordinarios de un solo volumen. Abre un ZIP dividido estándar desde su `.zip` final, un conjunto WinRAR moderno desde su primer `partN.rar` y un ZIP o 7Z numerado desde su volumen `.001`, con todos los volúmenes necesarios en el mismo directorio. El cifrado de nombres al crear y la edición interna de 7Z, RAR o TAR no están disponibles.

### Uso

1. Instala Archive Manager y actívalo en el Centro de plugins de AutoJs6.
2. Pulsa la acción principal de un archivo compatible o elige Abrir archivo, y navega como por una carpeta con la barra de ruta del host.
3. Usa la acción de extracción de la barra de ruta para la carpeta interna actual o la acción de codificación para corregir nombres ZIP, mantén pulsado para extraer una selección o elige Extraer en... en el menú del archivo para extraerlo completo. La contraseña se solicita cuando hace falta.
4. Elige Comprimir... para un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa la acción de la barra inferior. Para limpiar las fuentes después del éxito, activa expresamente la opción desactivada de forma predeterminada que las mueve a la papelera tras comprimir.
5. Elige Gestionar archivo... solo para añadir, renombrar o eliminar contenido de un ZIP ordinario de un solo volumen.

### Permisos y datos

Archive Manager no solicita permisos de almacenamiento ni de red. El host entrega descriptores de solo lectura de corta duración y transacciones vinculadas al UID del plugin, por lo que este no puede elegir rutas arbitrarias. Explorer Action v11 transporta la contraseña solo en una solicitud síncrona acotada y nunca la persiste. Explorer Action v12 añade únicamente un catálogo acotado y ligado a la sesión de volúmenes hermanos aprobados, con ID opacos y revalidación de identidad. Explorer Action v13 solo reindexa la misma fuente preparada y conserva el estado anterior hasta que el índice completo está listo. Explorer Action v14 solo coincide con sufijos compuestos acotados como `.zip.001` y `.7z.001`, nunca con cualquier archivo `.001`, y reutiliza el catálogo v12 sin conceder acceso a rutas o directorios. Las rutas peligrosas siguen aisladas, la salida se verifica antes de publicarse y confirmar un presupuesto nunca desactiva la seguridad estructural.

Explorer Action v15 agrupa solo salidas nuevas verificadas de una sesión en un lote recuperable de hasta 128 miembros. Explorer Action v16 permite que el host revalide fuentes y salidas y mueva las fuentes a la papelera solo cuando el plugin aporta la selección original completa y ordenada y todas las transacciones de salida confirmadas. El host sincroniza una copia recuperable y persiste su registro antes de retirar datos de origen; el plugin no recibe rutas arbitrarias ni eliminación directa. Si se pierde una respuesta Binder se consulta el mismo resultado terminal idempotente, sin repetir el movimiento.

### Roadmap

El trabajo restante se sigue con casillas verificables: reconstrucciones editables más allá de ZIP, deshacer en grupo e historial de la papelera, accesibilidad, el resto de la matriz de dispositivos y productores y el material para la primera publicación pública.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### v2.9.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v16 (código de versión 5276 o posterior)
- `Añadido` El formulario de compresión añade la opción desactivada de forma predeterminada para mover las fuentes a la papelera tras comprimir, y solo se ejecuta después de verificar y confirmar todas las salidas físicas
- `Añadido` Explorer Action v16 acepta únicamente la selección original completa y ordenada y todas las transacciones de salida confirmadas; después el host vuelve a validar las identidades antes de usar su papelera
- `Corregido` El host ahora sincroniza una copia recuperable y persiste su entrada de papelera antes de retirar una fuente; si un directorio solo se retira en parte, conserva la copia recuperable
- `Mejorado` La fase de papelera no se puede cancelar y distingue los resultados confirmado, requiere recuperación, fallido y desconocido; si se pierde una respuesta Binder se consulta el estado terminal del host sin repetir a ciegas

#### v2.8.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v15 (código de versión 5276 o posterior)
- `Añadido` Los ZIP divididos estándar y Comprimir cada elemento por separado ahora terminan la escritura y la verificación de lectura de todas las salidas antes de una publicación por lote recuperable de Explorer Action v15
- `Corregido` La creación de varias salidas ya no deja resultados parciales confirmados en fallos normales; Explorer se actualiza y se informa del éxito solo después de confirmar el lote completo
- `Corregido` Los interruptores de las opciones de compresión ahora se dibujan correctamente y se pueden pulsar en Android 7, en lugar de aparecer solo como etiquetas
- `Mejorado` El host registra de forma duradera el directorio padre y la identidad de cada archivo temporal antes de publicar; un fallo o reinicio revierte solo los miembros coincidentes y conserva los archivos modificados externamente para recuperación manual

#### v2.7.0

_2026/08/27_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v14 (código de versión 5276 o posterior)
- `Añadido` Los conjuntos numerados completos `.zip.001` y `.7z.001` ya se pueden explorar, previsualizar y extraer abriendo su volumen `.001`; permanecen de solo lectura
- `Añadido` Explorer Action v14 añade coincidencia acotada de sufijos compuestos y reutiliza la fuente de volúmenes hermanos v12 vinculada al UID, sin coincidir con archivos `.001` arbitrarios
- `Corregido` Android 7 combina los volúmenes ZIP numerados autorizados por el host en un archivo local privado antes de usar la ruta compatible con Zip4j, por lo que los conjuntos válidos ya no se notifican como dañados
- `Mejorado` Los números de volúmenes hermanos se limitan de `.002` a `.128` y todos los volúmenes presentes deben ser contiguos; el lector informa el siguiente ausente, revalida la identidad al materializar y nunca anuncia modificación interna

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
