ChatHack : service de chat complètement légal !

Introduction :

Ce document décrit le protocole ChatHack. Le protocole permet à des clients de communiquer avec un serveur ChatHack. Via ce serveur, les clients peuvent s'échanger des messages et demander à établir des connexions privées avec les autres clients connectés. Sur ces connexions privées, les clients peuvent échanger des messages et des fichiers. Tous les transferts de données se font au-dessus de connexions TCP.
Les entiers sont tous signés en BigEndian.
Les clients se connectent à un serveur. Chaque client connecté est identifié par un pseudonyme.
Le protocole permet deux formes d'accès : un accès authentifié par un mot de passe et un accès sans mot de passe. On suppose que le serveur a accès sous une forme ou une autre à une base de données de pseudonymes et de mots de passe. Le protocole ne s'occupe pas de la création et de la mise à jour de cette base. Le serveur garantit que deux clients ne peuvent pas avoir le même pseudonyme et qu'un client authentifié sans mot de passe n'utilise pas le pseudonyme d'un utilisateur de la base de données. Une fois connectés et identifiés par un pseudonyme, les clients peuvent :
•	Envoyer des messages qui seront transmis à tous les clients connectés.
•	Envoyer des messages privés et des fichiers à un autre client.
Le protocole permet aux clients de faire des demandes de communication privée et d'accepter/refuser la demande de communication privée.
Une communication privée de fait de la manière suivante :
Un client A effectue une demande de communication privée avec un autre client B.
Si B accepte, B devient hébergeur d’un nouveau serveur (s’il ne l’est pas déjà), et A se connecte au serveur de B.
Les messages (et fichiers) entre A et B passent par le serveur de B.
Si un autre client C souhaite démarrer une nouvelle communication privée avec B, C se connecte au serveur de B et les deux peuvent communiquer entre eux.
Le protocole gère également les logs, les warnings et les erreurs.
Les téléchargements de fichier se font par bloc de 1024 octets.
Les restrictions du type taille maximum des messages, système d’authentification, nombre maximum de clients connectés, contenu des « messages divers »… sont libres à l’implémentation.
Table des matières

Messages provenant du client
1 - Demande de connexion au serveur	3
2 - Envoi d’un message public à tous les clients connectés au serveur	4
3 - Demande d’effectuer une communication privée avec un autre client	4
4 - Réponse à la demande de communication privée avec un autre client	5
5 - Envoi d’un message privé à un autre client	5
6 - Envoi d’un fichier à un autre client – Initialisation	6
7 - Envoi d’un fichier à autre client – Téléchargement	6
Messages à destination du client
8 - Confirmation ou refus de la connexion du client au serveur	7
9 - Envoi d’un message public de la part d’un client vers tous les autres clients connectés	7
10 - Notification d’un client qu’un autre client souhaite effectuer une communication privée	8
11 - Réponse à la demande de communication privée	9
12 - Messages divers	10

 
Messages provenant du client

1 - Demande de connexion au serveur

Voici comment initialiser une connexion avec le serveur.
Il y a deux façons de se connecter au serveur. Soit le client est enregistré et possède un identifiant et un mot de passe, soit il ne possède pas de mot de passe et peut ainsi se connecter en tant « qu’invité ». Il doit alors saisir un identifiant qui n’est pas déjà dans la base de données et qui soit différent de tous les identifiants des clients déjà connecté au serveur.
Le type d’authentification est déterminé par le deuxième octet de la trame TCP. Il vaut soit 0 (authentification avec mot de passe) soit 1 (authentification sans mot de passe). Dans ce deuxième cas le client ne doit pas renseigner de mot de passe.

Format de la trame – authentification avec mot de passe :

1 byte       1 byte         4 bytes      N bytes       4 bytes           M bytes
+---------+--------------+--------------+----------+-----------------+--------------+
|    0    |       0      |   nameSize   |   name   |   passwordSize  |   password   |
+---------+--------------+--------------+----------+-----------------+--------------+

Format de la trame – authentification sans mot de passe :

1 byte       1 byte         4 bytes      N bytes
+---------+--------------+--------------+----------+
|    0    |       1      |   nameSize   |   name   |
+---------+--------------+--------------+----------+

Avec :
•	nameSize : Taille N du pseudo du client encodé en UTF-8
•	name : Pseudo du client encodé en UTF-8 de taille N
•	passwordSize : Taille M du mot de passe du client encodé en UTF-8
•	password : Mot de passe du client encodé en UTF-8 de taille M

Après l’avoir envoyé le client doit ensuite attendre la réponse du serveur pour savoir si sa connexion a été acceptée ou non.
 
2 - Envoi d’un message public à tous les clients connectés au serveur

Pour effectuer cette action ainsi que toutes les autres, le client devra être préalablement connecté au serveur.
Il enverra une trame respectant ce format :

1 byte     4 bytes     N bytes
+--------+-------------+---------+
|    1   | messageSize | message |
+--------+-------------+---------+

Avec :
•	messageSize : Taille N du message encodé en UTF-8
•	message : Message encodé en UTF-8 de taille N

Ensuite le serveur fera parvenir le message à tous les utilisateurs.


3 - Demande d’effectuer une communication privée avec un autre client

Pour réaliser une demande de communication privée, le client envoie une trame avec l’opcode = 2 ainsi que le pseudo du destinataire.

1 byte     4 bytes  N bytes
+----------------------------+
|    2   | nameSize |  name  |
+----------------------------+

Avec :
•	nameSize : Taille N du pseudo du destinataire encodé en UTF-8
•	name : Pseudo du destinataire encodé en UTF-8 de taille N

Le client devra alors attendre la réponse du destinataire, et si ce dernier accepte, un serveur sera créé au niveau du client destinataire de la communication privée (si pas déjà existant).
 
4 - Réponse à la demande de communication privée avec un autre client

Après avoir reçu la demande d’ouverture de communication privée, le client peut renvoyer une trame pour l’accepter ou la refuser. S’il accepte, il créera un serveur (si ce n’est pas déjà fait) puis indiquera son numéro de port et attribuera au demandeur un identifiant unique, qu’il utilisera à chaque communication.

Format de la trame – Acceptation de la communication privée :

1 byte   1 byte     4 bytes      N Bytes    4 bytes   4 bytes
+--------------------------------------------------------------+
|    3   |   0   |   nameSize   |   name   |   port   |   id   |
+--------------------------------------------------------------+

Format de la trame – Refus de la communication privée :

1 byte   1 byte     4 bytes      N Bytes
+------------------------------------------+
|    3   |   1   |   nameSize   |   name   |
+------------------------------------------+

Avec :
•	nameSize : Taille N du pseudo du destinataire encodé en UTF-8
•	name : Pseudo du destinataire encodé en UTF-8 de taille N
•	port : Numéro du port du serveur auquel le client demandeur peut se connecter
•	id : Identifiant unique pour le client demandeur


5 - Envoi d’un message privé à un autre client

Une fois la connexion établie entre les deux clients, il sera possible d’envoyer un message entre ces deux utilisateurs uniquement. Il faudra pour cela envoyer une trame de ce type :

1 byte   4 bytes     4 bytes     N bytes
+------------------------------------------+
|   4   | clientId | messageSize | message |
+------------------------------------------+

Avec :
•	clientId : Identifiant du client
•	messageSize : Taille N du message encodé en UTF-8
•	message : Message encodé en UTF-8 de taille N
 
6 - Envoi d’un fichier à un autre client – Initialisation

Il sera également possible d’envoyer des fichiers en message privé aux autres clients.
Cette trame va servir à initialiser l’envoi du fichier.
Pour cela il faut définir quelques paramètres.
Le destinataire connaîtra ainsi la taille du fichier et devra attendre d’avoir reçu toutes les données avant de pouvoir finir le téléchargement.

1 byte   4 bytes     4 bytes      N bytes    4 bytes   4 bytes
+-------+----------+--------------+----------+----------+--------+
|   5   | clientId | fileNameSize | fileName | fileSize | fileId |
+-------+----------+--------------+----------+----------+--------+

Avec :
•	clientId : Identifiant unique du client envoyeur
•	fileNameSize : Taille N du nom du fichier encodé en UTF-8
•	fileName : Nom du fichier encodé en UTF-8 de taille N
•	fileSize : Taille du fichier
•	fileId : Identifiant du fichier


7 - Envoi d’un fichier à autre client – Téléchargement

Cette nouvelle trame sert à envoyer le fichier petit à petit. On va donc envoyer 1024 octets maximum à la fois.

1 byte    4 bytes  4 bytes   1024 bytes max
+--------+----------+--------+----------------+
|    6   | clientId | fileId |    fileData    |
+--------+----------+--------+----------------+

Avec :
•	clientId : Identifiant unique du client envoyeur
•	fileId : Identifiant du fichier
•	fileData : Données du fichier
 
Messages à destination du client

Comme pour le client, chaque trame envoyée par le/les serveur(s) débute par un code d’opération codé sur un octet.

8 - Confirmation ou refus de la connexion du client au serveur

Lorsqu’un client envoie sa demande de connexion, le serveur lui répond avec un opcode = 0 suivi d’un autre code indiquant que la connexion est acceptée ou qu’il y a eu un problème, et que dans ce cas la connexion n’est pas établie.

1 byte           1 byte
+---------+------------------+
|    0    |   responseCode   |
+---------+------------------+

Avec responseCode qui peut avoir les valeurs suivantes :
•	0 : Connexion acceptée
•	1 : Connexion refusée - Identifiants invalides
•	2 : Connexion refusée – Login déjà existant

9 - Envoi d’un message public de la part d’un client vers tous les autres clients connectés

Lorsqu’un client émet un message public, le serveur le redistribue à tous les autres clients connectés en leur envoyant un paquet de la forme suivante :

1 byte       4 bytes      N bytes        4 bytes        M Bytes
+---------+--------------+----------+-----------------+------------+
|    1    |   nameSize   |   name   |   messageSize   |   message  |
+---------+--------------+----------+-----------------+------------+

Avec :
•	nameSize : Taille N du pseudo de l’émetteur du message encodé en UTF-8
•	name : Pseudo de l’émetteur du message encodé en UTF-8 de taille N
•	messageSize : Taille M du message encodé en UTF-8
•	message : Message encodé en UTF-8 de taille M
 
10 - Notification d’un client qu’un autre client souhaite effectuer une communication privée

Lorsqu’un client effectue une demande de connexion privée avec un autre client, ce dernier reçoit une trame du serveur avec un opcode = 2 ainsi que le pseudo du demandeur.

1 byte      4 bytes      N bytes
+---------+--------------+----------+
|    2    |   nameSize   |   name   |
+---------+--------------+----------+

Avec :
•	nameSize : Taille N du pseudo du demandeur encodé en UTF-8
•	name : Pseudo du demandeur encodé en UTF-8 de taille N
 
11 - Réponse à la demande de communication privée

Lorsqu’un client effectue une demande de connexion privée avec un autre client, ce dernier reçoit une trame du serveur avec un opcode = 3, suivi d’un autre code spécifiant la réponse.
Ce code possède 3 valeurs : le destinataire a accepté la demande de communication privée, le destinataire l’a refusée, le destinataire demandé n’existe pas ou n’est pas connecté au serveur.
Si le destinataire a accepté, le client reçoit un identifiant et les informations pour se connecter au serveur du destinataire : adresse IP et port.

Format de la trame – Demande acceptée et IP version IPv4 :

1 byte   1 byte     4 bytes     N Bytes  1 byte   4 bytes   4 bytes   4 bytes
+--------+-------+--------------+---------+-------+--------+----------+--------+
|    3   |   0   |   nameSize   |   name  |   0   |   IP   |   port   |   id   |
+--------+-------+--------------+---------+-------+--------+----------+--------+

Format de la trame – Demande acceptée et IP version IPv6 :

1 byte   1 byte     4 bytes     N Bytes  1 byte  16 bytes   4 bytes   4 bytes
+--------+-------+--------------+---------+-------+--------+----------+--------+
|    3   |   0   |   nameSize   |   name  |   1   |   IP   |   port   |   id   |
+--------+-------+--------------+---------+-------+--------+----------+--------+

Format de la trame – Demande refusée :

1 byte         1 byte           4 bytes      N bytes
+---------+------------------+--------------+----------+
|    3    |   responseCode   |   nameSize   |   name   |
+---------+------------------+--------------+----------+

Avec :
•	responseCode : Valeurs possibles :
  o	1 : Le client destinataire a refusé la demande de communication privée
  o	2 : Le client destinataire n’existe pas ou n’est pas connecté au serveur
•	nameSize : Taille N du pseudo du client destinataire encodé en UTF-8
•	name : Pseudo du client encodé en UTF-8 de taille N
 
12 - Messages divers

Le serveur peut à tout moment envoyer un message à un ou plusieurs clients selon un évènement. Ainsi il peut envoyer :
-	Des messages à titre informatif, tels que les logs.
-	Des messages d’erreurs, tels que l’interruption de la connexion d’un client
-	Des messages d’avertissement : message trop long, fichier trop volumineux…
Chacun de ces messages est envoyé dans une trame portant l’opcode 4, suivi d’un autre code indiquant le type d’information, et enfin le texte de l’information.

1 byte       1 byte           4 bytes         N bytes
+---------+--------------+-----------------+-------------+
|    4    |   infoCode   |   messageSize   |   message   |
+---------+--------------+-----------------+-------------+

Avec :
•	infoCode : Indique le type de message :
  o	0 : Log
  o	1 : Warning
  o	2 : Erreur
•	messageSize : Taille N du message encodé en UTF-8
•	message : Message encodé en UTF-8 de taille N


Auteurs :
LIEGEY Armand
aliegey@etud.u-pem.fr
WADAN Samy
swadan@etud.u-pem.fr
Université Gustave Eiffel
Master 1 – Informatique
Groupe Apprentis
2019 - 2020
