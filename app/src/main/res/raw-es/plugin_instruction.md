Usa Archive Manager desde el gestor de archivos principal:

1. Instala y activa el plugin `Archive Manager`.
2. Abre el menú adicional de un archivo comprimido compatible.
3. Selecciona `Abrir archivo comprimido`.
4. Recorre carpetas, busca, usa la barra de ruta o abre entradas compatibles con los visores del host.
5. Atrás sube primero dentro del archivo antes de volver a la lista normal de archivos.

Para extraer de inmediato todo el archivo, selecciona `Extraer en...` en su menú y elige la carpeta de salida.

Para crear un archivo comprimido, selecciona `Comprimir...` en el menú de un archivo o carpeta normal. También puedes seleccionar varios elementos de la misma carpeta y usar `Comprimir...` en la barra inferior. Elige el nombre, el formato y el nivel de compresión; ZIP y 7Z permiten además una contraseña opcional.

La versión actual lee y extrae archivos de la familia ZIP, 7Z normales o solid, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 y TAR.ZST/TZST. Puede crear ZIP normales o divididos estándar, 7Z no solid, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 y TAR.ZST; ZIP y 7Z admiten contraseña opcional y los nombres de una salida cifrada siguen visibles. Los ZIP normales de un volumen, los 7Z de un volumen sin cifrar, no solid y dentro del presupuesto del decodificador, y los archivos TAR compatibles permiten añadir, renombrar y eliminar contenido mediante una reconstrucción completamente verificada. Los 7Z cifrados, solid, divididos, peligrosos, no compatibles o fuera del presupuesto y las entradas TAR especiales siguen siendo de solo lectura. Al abrir solo el volumen final de un ZIP dividido estándar `.z01 + .zip`, se indican los volúmenes anteriores necesarios; al crear se puede elegir un tamaño MiB predefinido o personalizado. La salida se divide si supera el tamaño elegido; una salida menor permanece como un único `.zip`.

El plugin no solicita acceso general al almacenamiento ni a la red. Los archivos normales se exploran mediante canales con posición independiente sobre el descriptor de solo lectura del host, sin copiar todo el archivo; las entradas o lectores incompatibles (actualmente incluido ZIP cifrado) recurren a la caché privada, que se limpia al cerrar o caducar. La creación de archivos lee mediante una sesión breve del host vinculada al plugin y solo puede escribir una salida transaccional en la carpeta padre actual. Las comprobaciones de recorrido de rutas, tamaño de origen y límites de salida permanecen activas.
