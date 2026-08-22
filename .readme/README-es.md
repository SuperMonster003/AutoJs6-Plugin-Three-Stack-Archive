<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>Complemento del gestor de archivos de AutoJs6 para abrir, extraer y crear archivos ZIP</p>

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

Archive Manager integra la exploración, extracción y creación de ZIP en el gestor de archivos de AutoJs6. La versión actual explora los archivos en la lista nativa del host con rutas externas e internas, muestra vistas previas de las entradas compatibles y puede comprimir un elemento o una selección con el mismo directorio padre. Más formatos, la extracción por entrada y la edición interna siguen en el Roadmap.

### Disponible ahora

- Abrir archivos de la familia ZIP directamente en la lista nativa de AutoJs6, con el tema, el modo oscuro y los colores dinámicos del host.
- Mostrar el directorio externo, el nombre del archivo y el directorio interno en la barra de ruta; tocar un nivel para ir a él y usar Atrás para subir antes de salir del archivo.
- Abrir entradas compatibles de documentos, imágenes, audio y vídeo con los visores existentes del host.
- Usar el acceso «Extraer en...» para extraer todo el archivo sin abrir antes la vista de exploración.
- Explorar carpetas, buscar y ordenar el contenido del archivo.
- Crear la lista desde los metadatos sin descomprimir primero todas las entradas.
- Verificar la estructura ZIP real y unificar las capacidades de vista previa, extracción y creación, manteniendo desactivadas las opciones no compatibles.
- Admitir Zip64, preámbulos autoextraíbles, codificaciones antiguas y separadores de Windows.
- Cambiar la codificación de nombres ZIP cuando la detección automática sea incorrecta; la exploración y la extracción reutilizan la misma selección.
- Mostrar los fallos con formato, etapa, código estable y motivo claro; las compilaciones de depuración permiten copiar el diagnóstico completo.
- Ofrecer «Comprimir...» para archivos, carpetas y selecciones múltiples con el mismo directorio padre.
- Crear ZIP con nombre y nivel de compresión configurables; usar por defecto el nombre del objetivo para un elemento y el de la carpeta padre para varios.
- Escribir primero en un archivo temporal del mismo directorio y confirmar de forma atómica; numerar conflictos sin sobrescribir archivos existentes.

### Formatos actuales

La versión actual reconoce estas extensiones de la familia ZIP:

```text
zip, jar, aar, war
```

La versión actual puede crear estos formatos:

```text
zip
```

> La exploración nativa y la vista previa de entradas de Explorer Action v6, junto con la compresión v4, requieren AutoJs6 con código de versión 5276 o posterior. La extracción por entrada, 7z, variantes tar, contraseñas, volúmenes divididos, cifrado de nombres, archivos separados, eliminación de fuentes y añadir/eliminar dentro del archivo aún no son funciones publicadas. El Roadmap es la referencia.

### Uso

1. Instala el complemento y actívalo en el centro de complementos de AutoJs6.
2. Abre el menú de un archivo ZIP, JAR, AAR o WAR.
3. Elige «Abrir archivo comprimido» y entra en carpetas, busca o navega con la barra de ruta en la lista del host.
4. Para extraer todo el archivo, elige «Extraer en...» en su menú y selecciona una carpeta con el selector del sistema Android.
5. Para crear un ZIP, elige «Comprimir...» en el menú de un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa «Comprimir...» en la barra inferior.

### Permisos y datos

El complemento no solicita permisos de almacenamiento ni de red. La exploración nativa usa una sesión breve de archivo de solo lectura vinculada al UID del host y elimina la entrada temporal al cerrar o desvincular; la extracción solo usa el URI temporal del host. La creación de ZIP usa una sesión de archivos vinculada al UID del complemento, lee los objetivos por páginas y solo puede crear una salida transaccional en el directorio padre actual. Se eliminaron el límite fijo de 4 GiB y los umbrales durante la exploración; se mantienen el aislamiento de rutas, las comprobaciones de integridad y la limpieza de fallos.

### Roadmap

Las tareas y criterios para más formatos, contraseñas y volúmenes, extracción por entrada, edición de archivos y la matriz completa de dispositivos están en el Roadmap. Una casilla sin marcar no es una función actual.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### Unreleased

_Sin publicar_

- `Añadido` El producto pasa a llamarse Archive Manager y la acción, Abrir archivo comprimido
- `Añadido` Explorer Action v5 explora archivos en la lista nativa de AutoJs6 con la barra de ruta, el tema y la navegación Atrás existentes
- `Añadido` Explorer Action v6 abre entradas compatibles con los visores de documentos, imágenes, audio y vídeo del host
- `Añadido` Acceso Extraer en... para elegir destino y extraer todo el archivo
- `Añadido` Explorer Action v4 añade Comprimir... a los menús de archivos y carpetas y a la barra de cinco acciones para selecciones del mismo directorio padre
- `Añadido` Creación de ZIP con nombre predeterminado, niveles de compresión, progreso, cancelación y numeración automática de conflictos
- `Corregido` La lista ZIP usa metadatos y admite preámbulos autoextraíbles, codificaciones antiguas, separadores Windows y más métodos legibles
- `Corregido` La codificación de nombres ZIP se puede cambiar cuando la detección automática sea incorrecta y la extracción reutiliza la selección
- `Corregido` Los tamaños desconocidos, URI DocumentsProvider válidos y permisos de escritura adicionales del host ya no bloquean archivos válidos
- `Corregido` Corregida la exploración y extracción de ZIP en Android 7.x, que fallaba al llamar a API exclusivas de sistemas más recientes
- `Mejorado` Se eliminaron el límite fijo de 4 GiB y los umbrales de tamaño/ratio al explorar, manteniendo aislamiento y verificaciones
- `Mejorado` Se añadió un Roadmap verificable y se reescribieron README y CHANGELOG
- `Mejorado` La pantalla independiente sigue ahora el modo día/noche y los colores dinámicos Material
- `Mejorado` La salida ZIP pasa por una sesión del host vinculada al UID, usa un archivo temporal del mismo directorio y se confirma atómicamente sin permiso de almacenamiento ni sobrescrituras
- `Mejorado` Las capacidades del formato y de cada entrada se comprueban de forma uniforme al previsualizar, extraer y crear, por lo que las opciones no disponibles permanecen desactivadas
- `Mejorado` Los fallos identifican el formato, la etapa, un código estable y el motivo; las compilaciones de depuración permiten copiar el diagnóstico completo

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
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
