Usa el Gestor de archivos comprimidos desde el gestor de archivos principal:

1. Instala y activa el plugin `Archive Manager`.
2. Abre el menú adicional de un archivo comprimido compatible.
3. Selecciona `Abrir archivo comprimido`.
4. Recorre las carpetas o busca rutas de entradas, y selecciona archivos o carpetas.
5. Selecciona `Extraer selección` y elige una carpeta de salida con el selector del sistema Android.

Para extraer de inmediato todo el archivo, selecciona `Extraer en...` en su menú y elige la carpeta de salida.

Para crear un archivo comprimido, selecciona `Comprimir...` en el menú de un archivo o carpeta normal. También puedes seleccionar varios elementos de la misma carpeta y usar `Comprimir...` en la barra inferior. Elige el nombre, el formato y el nivel de compresión; solo ZIP permite además una contraseña opcional.

La versión actual lee y extrae archivos de la familia ZIP, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 y TAR.ZST/TZST. Puede crear ZIP, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST; la contraseña opcional solo está disponible para ZIP. Los enlaces, nodos de dispositivo y entradas dispersas TAR son de solo lectura. Los demás formatos, los volúmenes divididos y la edición interna permanecen en el Roadmap.

El plugin no solicita acceso general al almacenamiento ni a la red. La exploración guarda la entrada temporalmente en la caché privada. La creación de archivos lee mediante una sesión breve del host vinculada al plugin y solo puede escribir una salida transaccional en la carpeta padre actual. Las comprobaciones de recorrido de rutas, tamaño de origen y límites de salida permanecen activas.
