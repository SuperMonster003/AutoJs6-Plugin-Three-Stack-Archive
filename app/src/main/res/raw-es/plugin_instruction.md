Usa el Gestor de archivos comprimidos desde el gestor de archivos principal:

1. Instala y activa el plugin `Archive Manager`.
2. Abre el menú adicional de un archivo ZIP, JAR, AAR o WAR.
3. Selecciona `Abrir archivo comprimido`.
4. Recorre las carpetas o busca rutas de entradas, y selecciona archivos o carpetas.
5. Selecciona `Extraer selección` y elige una carpeta de salida con el selector del sistema Android.

Para extraer de inmediato todo el archivo, selecciona `Extraer en...` en su menú y elige la carpeta de salida.

Para crear un ZIP, selecciona `Comprimir...` en el menú de un archivo o carpeta normal. También puedes seleccionar varios elementos de la misma carpeta y usar `Comprimir...` en la barra inferior. Confirma el nombre y el nivel de compresión para crear el archivo en la carpeta actual.

La versión actual lee y extrae archivos de la familia ZIP y crea archivos ZIP. Las contraseñas, los volúmenes divididos, la edición interna y más formatos permanecen en el Roadmap.

El plugin no solicita acceso general al almacenamiento ni a la red. La exploración guarda la entrada temporalmente en la caché privada. La creación de ZIP lee mediante una sesión breve del host vinculada al plugin y solo puede escribir una salida transaccional en la carpeta padre actual. Las comprobaciones contra el recorrido de rutas y la salida fuera del destino permanecen activas.
