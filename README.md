# ChatHack - Serveur et client

Service de discussions et d'échanges de fichiers en JAVA

## Contenu du projet

- Les sources du projets
	- Client
	- Serveur
	- Contexts (inclus par le client et le serveur)
	- Ressoures (incluses par les Contexts)
- Un script d'installation
- La RFC
- La documentation au format Javadoc
- Un manuel utilisateur
- Un manuel développeur

## Installation

### Sous Linux

Rendre le script exécutable avec `chmod +x install.sh` puis l'exécuter.
Ceci compilera les sources et génèrera 2 fichiers jar dans un nouveau dossier *output*, un pour le client et un pour le serveur.

### Sous Windows

Compiler avec Maven les sources du projet dans cet ordre :  
chatHack_resources -> chatHack_contexts -> (chatHack_client ou chatHack_server)  
Le fichier jar du client se situera dans *chatHack_client/target/* et le fichier jar du serveur dans *chatHack_server/target/*  

## Démarrage

Avant tout, lancer le serveur de base de données (ServerMDP.jar) non inclus dans ce projet.  
Ce serveur de base de données <u>doit être lancé sur le port **7777**</u>.

Ensuite exécuter le jar du serveur en spécifiant le port auquel il doit être lancé :  
`java -jar chatHack_server-1.0.jar port`

Enfin autant de clients que souhaité peuvent être lancé en indiquant l'adresse du serveur (localhost si lancé en local), le numéro du port, un chemin vers lesquels les fichiers seront uploadés et téléchargés ainsi qu'un login de connexion et éventuellement un mot de passe :  
`java -jar chatHack_client-1.0.jar adresse port repertoire login [mot de passe]`

## Auteurs

WADAN Samy - swadan@etud.u-pem.fr  
LIEGEY Armand - aliegey@etud.u-pem.fr  

Université Paris-Est Marne-la-Vallée  
Master 1 – Informatique  
Groupe Apprentis  
2019 - 2020  

## Version

Version initiale 1.0

## Licence

Le projet est sous la licence MIT.
