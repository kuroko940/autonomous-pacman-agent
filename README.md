# Projet Intelligence Artificielle : Pac-Man (Environnement Partiellement Observable)

## Contexte et Objectif
L'objectif de ce projet est de créer une intelligence artificielle (IA) capable de jouer à une variante du jeu de Pac-Man. 

La grande spécificité de cette version est qu'il s'agit d'un environnement partiellement observable. Les fantômes ne sont visibles que lorsque le Pac-Man les a directement dans son champ de vision. S'il y a un mur entre le Pac-Man et le fantôme, ce dernier devient invisible. L'IA doit donc déduire et anticiper les positions possibles des fantômes.

## Algorithme et Stratégie
* Algorithme principal : Le projet requiert impérativement l'implémentation de l'algorithme AND-OR.
* Prise de décision : L'IA est implémentée dans la classe `AI`, qui utilise la fonction `findNextMove` pour calculer la prochaine action ("UP", "DOWN", "LEFT" ou "RIGHT") à partir de l'état de croyance courant.
* Performance : L'objectif est de maximiser le score obtenu tout en minimisant le temps de calcul de l'IA (le temps d'exécution moyen est évalué).

## Architecture du Code
Le projet s'articule autour de plusieurs classes Java fournies pour modéliser le jeu :
* `BeliefState` : Modélise un ensemble de croyances regroupant les états dans lesquels peut se trouver le jeu. Cette classe permet notamment d'inférer la position des fantômes invisibles en fonction de leurs derniers déplacements connus.
* `AI` : La classe contenant la logique décisionnelle principale.
* `Result` : Gère une liste d'états de croyance résultant d'une action du Pac-Man, en séparant les états qui ne correspondent pas au même percept (ce que l'agent voit ou non).
* `Plans` : Représente les résultats possibles d'un ensemble d'actions effectuées à partir d'un même état de croyance. 
* `Position` : Permet de manipuler les coordonnées (ligne, colonne) et la direction des entités sur le plateau.

## Règles du Jeu et Scores
Votre but en tant que Pac-Man est d'obtenir le plus grand score possible en survivant aux fantômes.
* Gommes classiques : Chaque gomme mangée rapporte 10 points.
* Super-gommes : Elles ont le pouvoir d'effrayer les fantômes pendant un certain temps (leurs couleurs virent au bleu). 
* Manger un fantôme : Lorsqu'un fantôme est effrayé, Pac-Man peut le dévorer. Cela rapporte 100 points par fantôme. Le fantôme n'est pas tué mais retourne à sa position de départ en perdant son statut effrayé.
* Déplacements : Pac-Man peut se déplacer en haut, en bas, à gauche ou à droite, mais ne peut pas traverser les murs.
