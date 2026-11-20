3-Stack Archive añade la gestión de archivos comprimidos al administrador de archivos de AutoJs6: explora archivos ZIP, 7Z, RAR y de la familia TAR como si fueran carpetas, extrae todo o parte de su contenido y comprime archivos y carpetas. La pantalla de inicio del plugin también puede abrir archivos comprimidos para explorarlos y extraerlos por sí sola.

### Primeros pasos

1. Activa 3-Stack Archive en "Centro de plugins".
2. En el administrador de archivos de AutoJs6, abre el menú de un archivo comprimido y elige "Ver archivo"; su contenido aparece en la misma lista de archivos.
3. Entra en carpetas, busca o toca una entrada para abrirla en un visor de AutoJs6; el botón Atrás vuelve primero al nivel superior dentro del archivo comprimido.
4. Para sacar archivos, elige "Extraer en..." en el menú del archivo comprimido para extraerlo entero, o mantén pulsadas las entradas para seleccionarlas y extraer solo esas.

### Funciones principales

- **Explorar y previsualizar**: Explora archivos ZIP, JAR, 7Z, RAR y de la familia TAR en la lista de archivos de AutoJs6, con búsqueda y barra de ruta; documentos, imágenes, audio y vídeo se abren en un visor sin extraer antes.
- **Abrir como archivo comprimido**: Si no se reconoce el nombre de un archivo, elige "Abrir como archivo comprimido..." en su menú; el formato se detecta por el contenido y se muestra en la barra de ruta.
- **Extracción**: Extrae el archivo completo, la carpeta actual (el botón a la derecha de la barra de ruta) o las entradas seleccionadas, con progreso y cancelación; los archivos con el mismo nombre se pueden omitir, sobrescribir o numerar automáticamente.
- **Contraseñas y codificación de nombres**: Los archivos ZIP, 7Z y RAR cifrados piden una contraseña al abrirlos o extraerlos, y una contraseña incorrecta se puede reintentar al momento; los nombres ZIP ilegibles se corrigen cambiando la codificación en la barra de ruta.
- **Archivos divididos**: Los conjuntos completos `.z01` + `.zip`, `partN.rar`, `.zip.001` y `.7z.001` se pueden explorar, previsualizar y extraer, pero no modificar.
- **Compresión**: Elige "Comprimir..." para un archivo, una carpeta o varios elementos seleccionados en una misma carpeta y crea archivos ZIP, 7Z, TAR, TAR.GZ, etc.; ZIP y 7Z admiten contraseña y ZIP se puede dividir por tamaño.
- **Un archivo por elemento**: Con varios elementos seleccionados, activa "Comprimir cada elemento por separado" para crear un archivo comprimido por elemento; los conflictos de nombres se numeran automáticamente.
- **Modificar archivos**: Los ZIP normales de un solo volumen, los 7Z sin cifrar y sin solid y los de la familia TAR admiten añadir archivos y carpetas, crear carpetas vacías, renombrar y eliminar entradas; tras un cambio, "Restaurar versión anterior" lo deshace una vez.
- **Orígenes a la papelera**: Con "Mover los elementos de origen a la papelera después de comprimir" activado (desactivado por defecto), los orígenes van a la papelera de AutoJs6 solo tras verificar todas las salidas; "Historial de recuperación de fuentes", en el menú de la página de compresión, los restaura.
- **Pantalla de inicio**: Abre el plugin desde el lanzador y toca "Abrir un archivo comprimido" para explorar y extraer archivos; "Ajustes" cambia el idioma, el modo oscuro, el color del tema y el icono del lanzador.

### Uso en scripts

Las herramientas de plugin `archive_list`, `archive_read` y `archive_extract` enumeran el contenido de un archivo comprimido, leen un archivo de texto que contiene y lo extraen desde un script:

```js
let list = plugins.tools.call('archive_list', { archive: './demo.zip' });
list.result.entries.forEach(e => console.log(e.path, e.size));

let readme = plugins.tools.call('archive_read', { archive: './demo.zip', path: 'README.md' });
console.log(readme.result.text);

let extracted = plugins.tools.call('archive_extract', { archive: './demo.zip' }, {
    outputs: { files: './out/demo' },
});
console.log(extracted.result.filesExtracted, extracted.result.kept);
```

- Las rutas relativas parten del directorio de trabajo de AutoJs6; la carpeta de destino de una extracción puede existir ya, y sus archivos existentes se conservan y se cuentan en `kept`.
- `archive_read` devuelve texto UTF-8, como máximo 64 KiB por llamada; un archivo binario no devuelve texto y `binary` es `true`.
- Pasa la contraseña de un archivo cifrado en `password`; las herramientas de plugin no admiten archivos divididos.

### Requisitos

- Android 7.0 o posterior.
- AutoJs6 6.8.0 o posterior, con este plugin activado en "Centro de plugins"; la pantalla de inicio del plugin también funciona sin AutoJs6.
- El plugin no solicita permisos de almacenamiento ni de red y solo trata los archivos que eliges en AutoJs6 o en el selector de archivos del sistema.

### Preguntas frecuentes

**Casos en que las acciones del plugin no aparecen en el menú**

Comprueba que el plugin está activado en "Centro de plugins". Algunas versiones de AutoJs6 agrupan las acciones de los plugins en un submenú con el nombre del plugin, "3-Stack Archive"; para un archivo cuyo nombre no se reconoce, usa "Abrir como archivo comprimido...".

**Motivos por los que un archivo comprimido es de solo lectura**

Los archivos RAR, divididos, JAR/AAR/WAR y los 7Z cifrados o solid solo se pueden explorar y extraer. "Gestionar archivo..." muestra el formato del archivo actual y el motivo de la solo lectura.

**Nombres de archivo en archivos cifrados**

El cifrado ZIP y 7Z protege el contenido de los archivos, pero sus nombres siguen visibles dentro del archivo comprimido; por eso conviene no poner información sensible en los nombres.

**Casos en que un archivo dividido no se abre**

Abre un ZIP dividido estándar desde su `.zip` final, un conjunto WinRAR desde su primer `partN.rar` y un conjunto numerado desde su archivo `.001`; todos los volúmenes deben estar en la misma carpeta.

Todos los parámetros y resultados de las herramientas de plugin se describen en [Documentación de AutoJs6 > Plugins](https://docs.autojs6.com/#/plugins); más información en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stack-Archive).
