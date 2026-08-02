Utilisez l'explorateur d'archives depuis l'explorateur AutoJs6 principal:

1. Installez et activez le plugin `Archive Browser`.
2. Ouvrez le menu supplémentaire d'un fichier ZIP, JAR, AAR ou WAR.
3. Sélectionnez `Parcourir l'archive`.
4. Parcourez les dossiers ou recherchez des chemins d'entrée, puis sélectionnez des fichiers ou des dossiers.
5. Sélectionnez `Extraire la sélection` et choisissez un dossier de sortie avec le sélecteur système Android.

Le plugin reçoit un accès temporaire en lecture seule au content URI d'entrée. Il ne demande aucune autorisation de stockage ou de réseau, et écrit uniquement dans l'arborescence de sortie sélectionnée via Storage Access Framework.

L'entrée est copiée dans le cache privé et limitée à 4 GiB. Chaque archive est limitée à 20,000 entrées, 512 MiB par entrée décompressée, 2 GiB de données décompressées au total et un taux de compression de 1000:1. Les chemins non sûrs et les méthodes de compression non prises en charge sont bloqués.
