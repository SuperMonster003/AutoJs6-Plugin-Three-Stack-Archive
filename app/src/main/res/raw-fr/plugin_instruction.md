Utilisez le gestionnaire d'archives depuis le gestionnaire de fichiers principal:

1. Installez et activez le plugin `Archive Manager`.
2. Ouvrez le menu supplémentaire d'une archive prise en charge.
3. Sélectionnez `Ouvrir l'archive`.
4. Parcourez les dossiers ou recherchez des chemins d'entrée, puis sélectionnez des fichiers ou des dossiers.
5. Sélectionnez `Extraire la sélection` et choisissez un dossier de sortie avec le sélecteur système Android.

Pour extraire immédiatement toute l'archive, sélectionnez `Extraire vers...` dans son menu, puis choisissez le dossier de sortie.

Pour créer une archive, sélectionnez `Compresser...` dans le menu d'un fichier ou dossier ordinaire. Vous pouvez aussi sélectionner plusieurs éléments du même dossier et utiliser `Compresser...` dans la barre inférieure. Choisissez le nom, le format et le niveau de compression; seul ZIP accepte en plus un mot de passe facultatif.

La version actuelle lit et extrait les archives de la famille ZIP, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST. Elle crée des ZIP, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST; le mot de passe facultatif est réservé à ZIP. Les liens, nœuds de périphérique et entrées creuses TAR restent en lecture seule. Les autres formats, les volumes fractionnés et la modification interne restent dans le Roadmap.

Le plugin ne demande aucun accès général au stockage ni au réseau. La consultation place temporairement l'entrée dans le cache privé. La création d'archive lit via une session courte de l'hôte liée au plugin et ne peut écrire qu'une sortie transactionnelle dans le dossier parent actuel. Les contrôles du chemin, de la taille source et des limites de sortie restent actifs.
