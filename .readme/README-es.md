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

Archive Manager integra la exploración, extracción y creación de ZIP en el gestor de archivos de AutoJs6. La versión actual puede comprimir un elemento o una selección con el mismo directorio padre y escribe el resultado mediante una sesión de archivos controlada por el host. Más formatos, una página de archivo nativa del host y la edición interna siguen en el Roadmap.

### Disponible ahora

- Abrir archivos de la familia ZIP desde el menú de AutoJs6.
- Usar el acceso «Extraer en...» para extraer todo el archivo sin abrir antes la vista de exploración.
- Explorar carpetas, buscar rutas y seleccionar archivos o carpetas.
- Crear la lista desde los metadatos sin descomprimir primero todas las entradas.
- Admitir Zip64, preámbulos autoextraíbles, codificaciones antiguas y separadores de Windows.
- Mantener el archivo navegable si una entrada no puede extraerse.
- Extraer la selección a una carpeta elegida con el selector de Android, con progreso y cancelación.
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

> La integración Explorer Action v4 requiere AutoJs6 con código de versión 5276 o posterior. 7z, variantes tar, contraseñas, volúmenes divididos, cifrado de nombres, archivos separados, eliminación de fuentes y añadir/eliminar dentro del archivo aún no son funciones publicadas. El Roadmap es la referencia.

### Uso

1. Instala el complemento y actívalo en el centro de complementos de AutoJs6.
2. Abre el menú de un archivo ZIP, JAR, AAR o WAR.
3. Elige «Abrir archivo comprimido», explora o busca y selecciona el contenido.
4. Elige «Extraer selección» y la carpeta de salida; para todo el archivo, elige directamente «Extraer en...» en su menú.
5. Para crear un ZIP, elige «Comprimir...» en el menú de un archivo o carpeta, o selecciona varios elementos del mismo directorio y usa «Comprimir...» en la barra inferior.

### Permisos y datos

El complemento no solicita permisos de almacenamiento ni de red. La exploración y la extracción solo usan el URI temporal del host. La creación de ZIP usa una sesión breve vinculada al UID del complemento, lee los objetivos por páginas y solo puede crear una salida transaccional en el directorio padre actual. Se eliminaron el límite fijo de 4 GiB y los umbrales durante la exploración; se mantienen el aislamiento de rutas, las comprobaciones de integridad y la limpieza de fallos.

### Roadmap

Las tareas y criterios para más formatos, contraseñas y volúmenes, edición, una página de archivo nativa del host, la barra de ruta interna y la matriz completa de dispositivos están en el Roadmap. Una casilla sin marcar no es una función actual.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### Notas de la versión

#### Unreleased

_Sin publicar_

- `Añadido` El producto pasa a llamarse Archive Manager y la acción, Abrir archivo comprimido
- `Añadido` Acceso Extraer en... para elegir destino y extraer todo el archivo
- `Añadido` Explorer Action v4 añade Comprimir... a los menús de archivos y carpetas y a la barra de cinco acciones para selecciones del mismo directorio padre
- `Añadido` Creación de ZIP con nombre predeterminado, niveles de compresión, progreso, cancelación y numeración automática de conflictos
- `Corregido` La lista ZIP usa metadatos y admite preámbulos autoextraíbles, codificaciones antiguas, separadores Windows y más métodos legibles
- `Corregido` Los tamaños desconocidos, URI DocumentsProvider válidos y permisos de escritura adicionales del host ya no bloquean archivos válidos
- `Corregido` Corregida la exploración y extracción de ZIP en Android 7.x, que fallaba al llamar a API exclusivas de sistemas más recientes
- `Mejorado` Se eliminaron el límite fijo de 4 GiB y los umbrales de tamaño/ratio al explorar, manteniendo aislamiento y verificaciones
- `Mejorado` Se añadió un Roadmap verificable y se reescribieron README y CHANGELOG
- `Mejorado` La pantalla independiente sigue ahora el modo día/noche y los colores dinámicos Material
- `Mejorado` La salida ZIP pasa por una sesión del host vinculada al UID, usa un archivo temporal del mismo directorio y se confirma atómicamente sin permiso de almacenamiento ni sobrescrituras

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
