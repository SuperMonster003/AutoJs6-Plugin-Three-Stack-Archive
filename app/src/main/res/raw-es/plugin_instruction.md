Usa el Explorador de archivos desde el Explorador principal de AutoJs6:

1. Instala y activa el plugin `Archive Browser`.
2. Abre el menú adicional de un archivo ZIP, JAR, AAR o WAR.
3. Selecciona `Explorar archivo`.
4. Recorre las carpetas o busca rutas de entradas, y selecciona archivos o carpetas.
5. Selecciona `Extraer selección` y elige una carpeta de salida con el selector del sistema Android.

El plugin recibe acceso temporal de solo lectura al content URI de entrada. No solicita permisos de almacenamiento ni de red, y solo escribe en el árbol de salida que selecciones mediante Storage Access Framework.

La entrada se copia en la caché privada y tiene un límite de 4 GiB. Cada archivo admite hasta 20,000 entradas, 512 MiB por entrada sin comprimir, 2 GiB de datos totales sin comprimir y una relación de compresión de 1000:1. Se bloquean las rutas no seguras y los métodos de compresión no compatibles.
