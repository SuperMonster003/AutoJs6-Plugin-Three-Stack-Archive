<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Complemento del gestor de archivos de AutoJs6 para explorar, extraer y crear archivos ZIP, 7Z y de la familia TAR</p>

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

Archive Manager integra la exploración, extracción y creación de ZIP, 7Z y formatos de la familia TAR en el gestor de archivos de AutoJs6. La versión actual explora los archivos en la lista nativa del host con rutas externas e internas, muestra vistas previas de las entradas compatibles, extrae todo el archivo, la carpeta interna actual o la selección marcada desde la página de gestión y crea un formato compatible desde un elemento o una selección con el mismo directorio padre. La extracción por entrada dentro de la página nativa del anfitrión y la edición interna siguen en el Roadmap.

### Disponible ahora

- Abrir archivos de las familias ZIP, 7Z y TAR directamente en la lista nativa de AutoJs6, con el tema, el modo oscuro y los colores dinámicos del host.
- Mostrar el directorio externo, el nombre del archivo y el directorio interno en la barra de ruta; tocar un nivel para ir a él y usar Atrás para subir antes de salir del archivo.
- Abrir entradas compatibles de documentos, imágenes, audio y vídeo con los visores existentes del host.
- Usar el acceso «Extraer en...» para extraer todo el archivo sin abrir antes la vista de exploración.
- Elige «Extracción selectiva...» para abrir la página de gestión y extraer todo el archivo, la carpeta interna actual o la selección marcada; «Extraer en...» sigue siendo el acceso para el archivo completo.
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
- Reconocer la estructura ZIP dividida estándar `.z01 + .zip` y mostrar los nombres de los volúmenes necesarios en lugar de marcar el volumen final como dañado; la lectura y la creación de archivos divididos aún no están disponibles.
- Explorar y extraer ZIP protegidos con ZipCrypto o AES, reintentar una contraseña incorrecta en el mismo lugar y crear opcionalmente ZIP con AES-256 cuyos nombres siguen visibles; la creación cifrada exige confirmar la contraseña con el mismo valor.
- Cambiar la codificación de nombres ZIP cuando la detección automática sea incorrecta; la exploración y la extracción reutilizan la misma selección.
- Mostrar los fallos con formato, etapa, código estable y motivo claro; las compilaciones de depuración permiten copiar el diagnóstico completo.
- Ofrecer «Comprimir...» para archivos, carpetas y selecciones múltiples con el mismo directorio padre.
- Crear ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST con nombre configurable y solo los niveles y opciones de contraseña que admita el formato elegido.
- Escribir primero en un archivo temporal del mismo directorio y confirmar de forma atómica; elegir numeración automática o probar el nombre exacto y preguntar antes de reintentar con un número, sin sobrescribir archivos existentes. Un fallo cancela la transacción; si el host no puede confirmar la limpieza de la salida temporal, el formulario muestra la ruta prevista e impide otro intento.

### Formatos actuales

La versión actual reconoce estas extensiones explorables y extraíbles:

```text
zip, jar, aar, war, 7z, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

La versión actual puede crear estos formatos:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> La exploración nativa y la vista previa de entradas de Explorer Action v6, junto con la compresión v4, requieren AutoJs6 con código de versión 5276 o posterior. La extracción por entrada dentro de la página nativa del anfitrión, la creación de volúmenes divididos, el cifrado de nombres al crear, los archivos separados, la eliminación de fuentes y añadir/eliminar dentro del archivo aún no son funciones publicadas. El Roadmap es la referencia.

### Uso

1. Instala el complemento y actívalo en el centro de complementos de AutoJs6.
2. Abre el menú de un archivo ZIP, JAR, AAR, WAR, 7Z o de la familia TAR.
3. Elige «Abrir archivo comprimido» y entra en carpetas, busca o navega con la barra de ruta en la lista del host.
4. Para extraer todo el archivo, elige «Extraer en...» en su menú y selecciona una carpeta con el selector del sistema Android.
5. Para extraer un ámbito concreto, elige «Extracción selectiva...», navega o marca entradas en la página de gestión, pulsa «Extraer en...», elige el ámbito y después selecciona la carpeta de salida.
6. Antes de extraer en la página de gestión, elige cómo tratar los nombres de salida equivalentes. Preguntar cada vez permite aplicar una decisión de omitir, sobrescribir o renombrar automáticamente a todos los conflictos compatibles restantes.
7. Para crear un archivo, elige «Comprimir...» en el menú de un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa «Comprimir...» en la barra inferior; después elige el formato, los ajustes disponibles y si un nombre exacto no disponible se numera automáticamente o se confirma antes de reintentar.

### Permisos y datos

El complemento no solicita permisos de almacenamiento ni de red. La exploración nativa conserva primero el descriptor posicionable de solo lectura del host y proporciona canales con posición independiente para los archivos normales. Las tuberías, las fuentes escribibles o no posicionables, Android 7 y los lectores que requieren un archivo local legible por el proceso (actualmente ZIP cifrado) recurren a la caché privada. El descriptor o la caché se limpian al cerrar, desvincular, fallar o caducar. La extracción solo usa el URI temporal del host. La creación de archivos usa una sesión vinculada al UID del complemento, lee los objetivos por páginas y solo puede crear una salida transaccional en el directorio padre actual. Las contraseñas permanecen solo en búferes de memoria que pueden borrarse, nunca se escriben en Bundles, preferencias, registros ni diagnósticos, y se borran al sustituirlas, al terminar una tarea o al destruir la página. Se eliminaron el límite fijo de 4 GiB y los umbrales durante la exploración; se mantienen el aislamiento de rutas, las comprobaciones de tamaño de origen, las transacciones de salida y la limpieza de fallos.

Los presupuestos de recursos solo deciden cuándo avisar o pedir confirmación; nunca reducen la seguridad estructural. Tras confirmar, los límites de bytes y relación solo aumentan hasta los valores declarados por las entradas seleccionadas para esa extracción. El crecimiento no declarado, los cambios del origen y las diferencias de tamaño o CRC siguen cancelando y limpiando la salida.

Los nombres no seguros solo se muestran como texto de solo lectura detrás de identificadores opacos y nunca se convierten en rutas de salida.

### Roadmap

Las tareas y criterios para más formatos, volúmenes divididos, extracción por entrada dentro de la página nativa del anfitrión, edición de archivos y la matriz completa de dispositivos están en el Roadmap. Una casilla sin marcar no es una función actual.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### Unreleased

_Sin publicar_

- `Añadido` El producto pasa a llamarse Archive Manager y la acción, Abrir archivo comprimido
- `Añadido` Explorer Action v5 explora archivos en la lista nativa de AutoJs6 con la barra de ruta, el tema y la navegación Atrás existentes
- `Añadido` Explorer Action v6 abre entradas compatibles con los visores de documentos, imágenes, audio y vídeo del host
- `Añadido` Acceso Extraer en... para elegir destino y extraer todo el archivo
- `Añadido` Extracción selectiva... abre la página de gestión con los ámbitos de archivo completo, carpeta interna actual y selección marcada, manteniendo el acceso directo para todo el archivo
- `Añadido` Políticas de conflicto al extraer para preguntar, omitir, sobrescribir o renombrar automáticamente, con aplicación global, recuentos precisos y conservación de carpetas de salida existentes
- `Añadido` Explorer Action v4 añade Comprimir... a los menús de archivos y carpetas y a la barra de cinco acciones para selecciones del mismo directorio padre
- `Añadido` Creación de ZIP con nombre predeterminado, niveles de compresión, progreso, cancelación y numeración automática de conflictos
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
- `Corregido` Las contraseñas incorrectas ahora se clasifican de forma estable como PASSWORD/WRONG_PASSWORD y las entradas AES v2 con CRC almacenado igual a cero ya no se marcan erróneamente como dañadas
- `Corregido` La carpeta de extracción predeterminada de extensiones compuestas como TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST elimina ahora el sufijo completo en lugar de conservar `.tar`
- `Corregido` Los archivos con nombres que contienen recorridos al directorio padre, rutas absolutas, prefijos de unidad o caracteres de control siguen siendo explorables; esos nombres pasan a una carpeta aislada de solo lectura, conservan la vista previa cuando sus datos son legibles y deben omitirse explícitamente antes de extraer
- `Corregido` La extracción ya no sobrescribe archivos ni carpetas existentes cuando el proveedor de destino considera idénticos los nombres que solo difieren en mayúsculas o minúsculas o son equivalentes en Unicode; las carpetas de salida equivalentes se numeran automáticamente
- `Corregido` La cancelación o un fallo de extracción revierte la nueva raíz de salida en una fase de limpieza no cancelable; si el proveedor rechaza la eliminación, se muestran el nombre y el URI del posible residuo en lugar de solo un error genérico
- `Mejorado` Se eliminaron el límite fijo de 4 GiB y los umbrales de tamaño/ratio al explorar, manteniendo aislamiento y verificaciones
- `Mejorado` Se añadió un Roadmap verificable y se reescribieron README y CHANGELOG
- `Mejorado` La pantalla independiente sigue ahora el modo día/noche y los colores dinámicos Material
- `Mejorado` Las salidas ZIP, 7Z y TAR pasan por una sesión del host vinculada al UID, usan un archivo temporal del mismo directorio y se confirman atómicamente sin permiso de almacenamiento ni sobrescrituras
- `Mejorado` Las capacidades del formato y de cada entrada se comprueban de forma uniforme al previsualizar, extraer y crear, por lo que las opciones no disponibles permanecen desactivadas
- `Mejorado` Los fallos identifican el formato, la etapa, un código estable y el motivo; las compilaciones de depuración permiten copiar el diagnóstico completo
- `Mejorado` Los archivos normales ahora se exploran mediante canales con posición independiente sobre el descriptor de solo lectura del host, sin copiar todo el archivo; las entradas o lectores incompatibles (actualmente incluido ZIP cifrado) recurren a la caché privada, que se limpia al cerrar, fallar o caducar
- `Mejorado` Se unificaron las transacciones de salida al crear ZIP, 7Z y TAR; las fuentes ilegibles y los fallos al reservar, abrir, escribir o confirmar tienen etapas estables, mientras que una cancelación no confirmada cierra la sesión, muestra la ruta prevista e impide un reintento inseguro
- `Dependencia` Se añadió Zip4j 2.11.5 con licencia Apache 2.0 para flujos ZIP cifrados, creación AES-256 y la ruta de compatibilidad con Android 7.x
- `Dependencia` Se añadió XZ for Java 1.12 con licencia 0BSD para leer y escribir TAR.XZ/TXZ en Java puro sin ABI nativas
- `Dependencia` Se añadió zstd-jni 1.5.7-15 con licencia BSD para leer y escribir TAR.ZST/TZST; las cuatro ABI de Android superan las comprobaciones de alineación ELF de 16 KiB y RELRO

#### v1.0.1

_2026/08/08_

- `Corregido` Enlace de servicio vacío al activar el complemento
- `Mejorado` Nombre, descripción e instrucciones más simples

#### v1.0.0

_2026/08/02_

- `Añadido` Primera versión para explorar ZIP, JAR, AAR y WAR y extraer la selección
- `Añadido` Búsqueda, selección, progreso, cancelación, limpieza temporal e interfaz localizada

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
