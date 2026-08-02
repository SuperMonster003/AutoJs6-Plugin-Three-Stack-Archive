<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>Exploración de solo lectura de archivos de la familia ZIP y extracción SAF selectiva para el Explorador de AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas (Languages)

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### Introducción

******

El plugin AutoJs6 Archive Browser añade exploración de archivos en modo de solo lectura al Explorador de AutoJs6. Abre contenedores basados en ZIP en un visor jerárquico dedicado y extrae solo las entradas seleccionadas por el usuario a una carpeta de salida elegida mediante Android Storage Access Framework.

******

### Funciones

******

- Registra una acción adicional de solo lectura para un único archivo mediante el protocolo compartido `org.autojs.plugin.EXPLORER_ACTION`.
- Explora las carpetas del archivo y muestra tamaño sin comprimir, tamaño comprimido, CRC y fecha de modificación.
- Busca rutas de entradas normalizadas y permite seleccionar archivos, carpetas o todas las entradas visibles.
- Extrae las entradas seleccionadas a un árbol SAF elegido por el usuario con progreso y cancelación.
- Acepta contenedores ZIP, JAR, AAR y WAR que usen métodos de compresión ZIP compatibles.
- Copia la entrada de solo lectura a la caché privada y elimina los datos temporales al cerrar el visor.

******

### Formatos compatibles

******

La versión 1 reconoce estas extensiones de la familia ZIP:

```text
zip, jar, aar, war
```

******

### Interfaz del plugin

******

AutoJs6 descubre y ejecuta el plugin con las siguientes identidades:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

La versión 1 se limita a una acción adicional de solo lectura para un único archivo en el Explorador principal de AutoJs6.

******

### Seguridad

******

El plugin no solicita permisos de almacenamiento ni de red. El host concede acceso temporal de solo lectura al content URI de entrada, mientras que el acceso de salida se limita al directorio SAF elegido explícitamente por el usuario. Se rechazan rutas absolutas, recorridos al directorio superior, prefijos de unidad, barras inversas, Unicode no seguro, rutas duplicadas, conflictos entre archivos y directorios, métodos de compresión no compatibles, diferencias de tamaño y diferencias de CRC.

******

### Límites de seguridad

******

- Tamaño máximo de la entrada en caché: `4 GiB`.
- Número máximo de nodos de ruta del archivo, incluidos los directorios implícitos: `20,000`.
- Tamaño máximo del directorio central ZIP: `64 MiB`.
- Longitud máxima de la ruta normalizada: `1,024` caracteres.
- Profundidad máxima de la ruta: `64` segmentos.
- Tamaño máximo sin comprimir de una entrada: `512 MiB`.
- Tamaño máximo total sin comprimir: `2 GiB`.
- Relación de compresión máxima: `1000:1`.

******

### Historial de versiones

******

# v1.0.0

###### 2026/08/02

* `Función` Plugin Archive Browser con ID `archive-browser`, motor `explorer-action` y variante `default`
* `Función` Acción de Explorador de solo lectura para un único contenedor ZIP, JAR, AAR o WAR con navegación jerárquica, búsqueda de rutas y selección de entradas
* `Función` Extracción selectiva a un árbol SAF elegido por el usuario con progreso y cancelación, acceso temporal de solo lectura a la entrada y sin permisos de almacenamiento ni de red
* `Función` Límites de seguridad de 4 GiB de entrada, 20,000 nodos de ruta incluidos los directorios implícitos, 64 MiB para el directorio central ZIP, 512 MiB por entrada sin comprimir, 2 GiB de datos totales sin comprimir y una relación de compresión de 1000:1
* `Función` Validación de rutas no seguras, entradas duplicadas o en conflicto, métodos de compresión no compatibles, cambios en el origen, diferencias de tamaño y diferencias de CRC
* `Función` Metadatos, interfaz, instrucciones de uso, README y CHANGELOG localizados en español, francés, ruso, árabe, japonés, coreano, inglés, chino simplificado, chino tradicional de Hong Kong y chino tradicional de Taiwán

##### Para consultar más versiones

* [CHANGELOG-es.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Compilación Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Los parámetros de compilación proceden de `version.properties`. El SDK mínimo actual es 24 y el SDK de destino es 36.

******

### Estructura de recursos

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/values-*/plurals.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` localiza los metadatos del plugin y los textos fijos del explorador, mientras que `plurals.xml` localiza los textos que dependen de cantidades. `plugin_instruction.md` proporciona instrucciones visibles desde el host. `.python/generate_markdown.py` genera los archivos README y de cambios a partir de fuentes JSON.

******

### Enlaces

******

- Documentación de AutoJs6: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
