Utilisez Archive Manager depuis le gestionnaire de fichiers principal:

1. Installez et activez le plugin `Archive Manager`.
2. Ouvrez le menu supplémentaire d'une archive prise en charge.
3. Sélectionnez `Ouvrir l'archive`.
4. Parcourez les dossiers, recherchez, utilisez la barre de chemin ou ouvrez les entrées prises en charge avec les visionneuses de l'hôte.
5. Retour remonte d'abord dans l'archive avant de revenir à la liste de fichiers ordinaire.

Pour extraire immédiatement toute l'archive, sélectionnez `Extraire vers...` dans son menu, puis choisissez le dossier de sortie.

Pour créer une archive, sélectionnez `Compresser...` dans le menu d'un fichier ou dossier ordinaire. Vous pouvez aussi sélectionner plusieurs éléments du même dossier et utiliser `Compresser...` dans la barre inférieure. Choisissez le nom, le format et le niveau de compression; ZIP et 7Z acceptent aussi un mot de passe facultatif.

La version actuelle lit et extrait les archives de la famille ZIP, les 7Z ordinaires ou solid, TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 et TAR.ZST/TZST. Elle crée des ZIP ordinaires ou fractionnés standard, des 7Z non solid, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 et TAR.ZST; ZIP et 7Z acceptent un mot de passe facultatif et les noms d'une sortie chiffrée restent visibles. Les ZIP ordinaires à volume unique et les TAR, TAR.GZ/TGZ, TAR.XZ/TXZ ou TAR.BZ2/TBZ2 compatibles permettent d'ajouter, renommer et supprimer du contenu par reconstruction entièrement vérifiée; les liens, nœuds de périphérique et entrées creuses TAR restent en lecture seule. Lorsqu'un volume final ZIP fractionné standard `.z01 + .zip` est ouvert seul, les volumes précédents requis sont indiqués; la création propose une taille MiB prédéfinie ou personnalisée. La sortie est fractionnée si elle dépasse la taille choisie; une sortie plus petite reste un seul `.zip`. Les autres formats modifiables et le travail restant sur les volumes fractionnés restent dans le Roadmap.

Le plugin ne demande aucun accès général au stockage ni au réseau. Les archives ordinaires sont consultées par des canaux à position indépendante sur le descripteur en lecture seule de l'hôte, sans copie intégrale ; les entrées ou lecteurs incompatibles (actuellement les ZIP chiffrés) utilisent le cache privé, nettoyé à la fermeture ou après expiration. La création d'archive lit via une session courte de l'hôte liée au plugin et ne peut écrire qu'une sortie transactionnelle dans le dossier parent actuel. Les contrôles du chemin, de la taille source et des limites de sortie restent actifs.
