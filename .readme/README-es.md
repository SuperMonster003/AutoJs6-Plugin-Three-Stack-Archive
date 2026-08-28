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

- Abrir archivos de las familias ZIP, 7Z y TAR directamente en la lista nativa de AutoJs6, con el tema, el modo oscuro y los colores dinámicos del host.
- Mostrar el directorio externo, el nombre del archivo y el directorio interno en la barra de ruta; tocar un nivel para ir a él y usar Atrás para subir antes de salir del archivo.
- Extraer la carpeta interna actual desde la barra de ruta, o entrar en el modo de selección y extraer los archivos y carpetas marcados, sin salir de la página nativa del host; se muestra el progreso, la tarea se puede cancelar y el directorio padre se actualiza al terminar.
- Abrir entradas compatibles de documentos, imágenes, audio y vídeo con los visores existentes del host.
- Usar «Extraer en...» para extraer todo el archivo en la carpeta recomendada junto a él, o elegir otra carpeta con el selector del sistema Android; los nombres de carpeta equivalentes que ya existan se numeran de forma segura.
- Elige «Gestionar archivo...» para abrir la página de gestión, extraer todo el archivo, la carpeta interna actual o la selección marcada, o editar un ZIP normal de un solo volumen con «Añadir archivos...», «Añadir carpeta...», «Nueva carpeta...», «Cambiar nombre...» y «Eliminar»; «Extraer en...» sigue siendo el acceso para el archivo completo.
- Los cambios de ZIP se planifican antes de escribir, se reconstruyen en una salida pendiente propiedad del host, se releen por completo y reemplazan atómicamente el original solo tras verificarlos. La cancelación o un fallo deja intacta la fuente, y una confirmación correcta actualiza Explorer automáticamente.
- Para nombres de salida equivalentes, elige Preguntar cada vez, Omitir, Sobrescribir o Cambiar nombre automáticamente; Aplicar a todo resuelve los conflictos compatibles restantes y las carpetas de salida existentes siempre se numeran y conservan.
- Explorar carpetas, buscar y ordenar el contenido del archivo.
- Crear la lista desde los metadatos sin descomprimir primero todas las entradas.
- Explorar archivos normales mediante el descriptor posicionable de solo lectura del host y canales con posición independiente, sin copiar todo el archivo; las tuberías, las fuentes escribibles o no posicionables, Android 7 y los lectores que requieren un archivo local legible por el proceso (actualmente ZIP cifrado) recurren a la caché privada, que se elimina al cerrar.
- Elegir presupuestos de extracción Compatible, Estricto o Personalizado; los archivos que superan los umbrales de entradas, rutas, tamaño de salida o relación de compresión siguen disponibles para explorar y muestran el espacio estimado y los riesgos antes de pedir una confirmación única para escribir.
- Mostrar el progreso por elementos y bytes, el elemento actual, la velocidad y el tiempo restante estimado; una cancelación o un fallo revierte la nueva raíz de salida, y cualquier residuo que el proveedor no elimine se identifica por nombre y URI.
- Mover los nombres con recorridos al directorio padre, rutas absolutas, prefijos de unidad o caracteres de control a una carpeta Rutas no seguras visible en la barra de ruta; los datos legibles conservan la vista previa y la extracción completa exige omitir explícitamente esas entradas sin afectar a las normales.
- Explorar, previsualizar y extraer TAR sin comprimir; los enlaces simbólicos y físicos, los nodos de dispositivo y las entradas dispersas solo se muestran y nunca se escriben como archivos normales.
- Explorar, previsualizar y extraer TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 y TAR.ZST/TZST con las mismas rutas internas, el aislamiento de entradas especiales y las comprobaciones de integridad.
- Explorar, previsualizar y extraer 7Z normales o solid, incluidas cadenas comunes de compresión y filtros, además de entradas con contenido o cabecera cifrados; las contraseñas ausentes o erróneas reciben un diagnóstico explícito.
- Verificar la estructura ZIP/7Z/TAR real y unificar las capacidades de vista previa, extracción y creación, manteniendo desactivadas las opciones no compatibles.
- Admitir Zip64, preámbulos autoextraíbles, codificaciones antiguas y separadores de Windows.
- Explorar, previsualizar y extraer conjuntos `.z01 + .zip` completos mediante descriptores hermanos autorizados y acotados por el host; crear ZIP divididos con tamaños MiB predefinidos o personalizados e informar claramente de volúmenes ausentes o modificados.
- Explorar y extraer ZIP protegidos con ZipCrypto o AES, reintentar una contraseña incorrecta en el mismo lugar y crear opcionalmente ZIP con AES-256 cuyos nombres siguen visibles; la creación cifrada exige confirmar la contraseña con el mismo valor.
- Cambiar la codificación de nombres ZIP cuando la detección automática sea incorrecta; la exploración y la extracción reutilizan la misma selección.
- Mostrar los fallos con formato, etapa, código estable y motivo claro; las compilaciones de depuración permiten copiar el diagnóstico completo.
- Ofrecer «Comprimir...» para archivos, carpetas y selecciones múltiples con el mismo directorio padre.
- Crear un archivo por elemento de una selección con el mismo directorio padre; el formulario muestra la cantidad de salidas y los nombres derivados, y numera los nombres existentes o repetidos sin sobrescribir. Cada salida se confirma por separado; una cancelación o un fallo conserva e informa las salidas completadas y bloquea un reintento ambiguo de todo el lote.
- Crear ZIP normales o divididos estándar, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST con nombre configurable y solo los niveles y opciones de contraseña que admita el formato elegido.
- Escribir primero en un archivo temporal del mismo directorio y confirmar de forma atómica; elegir numeración automática o probar el nombre exacto y preguntar antes de reintentar con un número, sin sobrescribir archivos existentes. Tras reservar el nombre, se analiza una instantánea acotada del origen antes de abrir la salida temporal; el formulario distingue análisis, compresión, verificación y confirmación, y muestra el total de archivos, los bytes leídos y los tamaños desconocidos. Antes de publicarla, la salida aún oculta se vuelve a leer por completo para comprobar formato, entradas, tamaños, CRC y huellas del contenido. Un fallo de creación o verificación cancela la transacción; si el host no puede confirmar la limpieza, el formulario muestra la ruta prevista e impide otro intento.

### Formatos actuales

La versión actual reconoce estas extensiones explorables y extraíbles:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La versión actual puede crear estos formatos:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> La integración nativa requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v17 (código de versión 5276 o posterior). RAR y los archivos divididos son deliberadamente de solo lectura; la edición se limita a `.zip` ordinarios de un solo volumen. Abre un ZIP dividido estándar desde su `.zip` final, un conjunto WinRAR moderno desde su primer `partN.rar` y un ZIP o 7Z numerado desde su volumen `.001`, con todos los volúmenes necesarios en el mismo directorio. El cifrado de nombres al crear y la edición interna de 7Z, RAR o TAR no están disponibles.

### Uso

1. Instala el complemento y actívalo en el centro de complementos de AutoJs6.
2. Abre el menú de un archivo ZIP, JAR, AAR, WAR, 7Z o de la familia TAR.
3. Elige «Abrir archivo comprimido» y entra en carpetas, busca o navega con la barra de ruta en la lista del host.
4. Para extraer todo el archivo, elige «Extraer en...» en su menú. Usa la carpeta actual recomendada o elige otra con el selector del sistema Android y confirma la ruta exacta de salida.
5. Para extraer la carpeta interna actual, pulsa el botón de extracción a la derecha de la barra de ruta. Para extraer entradas concretas, mantén pulsada una entrada, marca archivos o carpetas y pulsa «Extraer» en la barra inferior. Usa «Gestionar archivo...» o «Extraer en...» cuando necesites una contraseña, corregir la codificación, confirmar rutas no seguras, configurar conflictos o elegir otra carpeta.
6. Para editar un ZIP normal de un solo volumen, elige «Gestionar archivo...» y usa «Añadir archivos...», «Añadir carpeta...» para importar un árbol completo, «Nueva carpeta...» para crear una carpeta vacía, «Cambiar nombre...» o «Eliminar». Espera a que terminen la reconstrucción, la verificación y el mensaje de éxito antes de salir de la página.
7. Antes de extraer en la página de gestión, elige cómo tratar los nombres de salida equivalentes. Preguntar cada vez permite aplicar una decisión de omitir, sobrescribir o renombrar automáticamente a todos los conflictos compatibles restantes.
8. Para crear un archivo, elige «Comprimir...» en el menú de un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa «Comprimir...» en la barra inferior. Para crear un archivo por elemento, activa «Comprimir cada elemento por separado», revisa la vista previa y crea; este modo siempre resuelve los conflictos con numeración automática segura. Para ZIP, elige Sin división, un valor MiB habitual o un entero personalizado de 1 a 4096 MiB; si la salida supera ese tamaño, contiene volúmenes `.z01`, `.z02`, ... y un `.zip` final, mientras que una salida menor permanece como un único `.zip`.

### Permisos y datos

Archive Manager no solicita permisos de almacenamiento ni de red. El host entrega descriptores de solo lectura de corta duración y transacciones vinculadas al UID del plugin, por lo que este no puede elegir rutas arbitrarias. Explorer Action v11 transporta la contraseña solo en una solicitud síncrona acotada y nunca la persiste. Explorer Action v12 añade únicamente un catálogo acotado y ligado a la sesión de volúmenes hermanos aprobados, con ID opacos y revalidación de identidad. Explorer Action v13 solo reindexa la misma fuente preparada y conserva el estado anterior hasta que el índice completo está listo. Explorer Action v14 solo coincide con sufijos compuestos acotados como `.zip.001` y `.7z.001`, nunca con cualquier archivo `.001`, y reutiliza el catálogo v12 sin conceder acceso a rutas o directorios. Explorer Action v17 solo añade una acción secundaria de solo lectura y sin coincidencias cuando no coincide la acción principal; reutiliza una sesión existente tras la elección del usuario y no concede acceso nuevo a rutas, directorios ni escritura. Las rutas peligrosas siguen aisladas, la salida se verifica antes de publicarse y confirmar un presupuesto nunca desactiva la seguridad estructural.

Explorer Action v15 agrupa solo salidas nuevas verificadas de una sesión en un lote recuperable de hasta 128 miembros. Explorer Action v16 permite que el host revalide fuentes y salidas y mueva las fuentes a la papelera solo cuando el plugin aporta la selección original completa y ordenada y todas las transacciones de salida confirmadas. El host sincroniza una copia recuperable y persiste su registro antes de retirar datos de origen; el plugin no recibe rutas arbitrarias ni eliminación directa. Si se pierde una respuesta Binder se consulta el mismo resultado terminal idempotente, sin repetir el movimiento.

### Roadmap

El trabajo restante se sigue con casillas verificables: reconstrucciones editables más allá de ZIP, deshacer en grupo e historial de la papelera, el resto de la matriz de dispositivos y productores y el material para la primera publicación pública.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### v2.10.0

_2026/08/28_

- `Nota` Esta versión requiere la compilación emparejada de AutoJs6 6.8.0 con Explorer Action v17 (código de versión 5276 o posterior)
- `Añadido` Un archivo cuyo nombre o extensión no se reconoce ahora puede usar Abrir como archivo comprimido...; tras detectar la estructura, la barra de ruta nativa indica el formato real cuando difiere del nombre
- `Añadido` Explorer Action v17 añade una acción secundaria de solo lectura y sin coincidencias, vinculada a una acción principal normal, y devuelve metadatos acotados del formato detectado desde la sesión existente
- `Corregido` TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST ahora usan sufijos exactos del nombre completo en vez de las extensiones genéricas `gz`, `xz`, `bz2` o `zst`, de modo que los flujos comprimidos ordinarios no reciben la acción principal de archivo
- `Mejorado` El host no examina archivos en segundo plano al crear los menús; solo una elección explícita ejecuta una llamada existente de apertura de solo lectura, sin autoridad nueva sobre rutas, directorios o escritura

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
