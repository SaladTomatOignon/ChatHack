#!/bin/bash

# Chemins des sources
ressources_dir='./chatHack_resources/'
contexts_dir='./chatHack_contexts/'
client_dir='./chatHack_client/'
server_dir='./chatHack_server/'

output_dir='./output/'
client_jarfile='chatHack_client-1.0.jar'
server_jarfile='chatHack_server-1.0.jar'

# Compile les sources
# Param 1 : Chemin du projet à générer.
# Param 2 : Boolean indiquant s'il faut générer un jar.
compile() {
	project_dir=$1
	generate_jar=$2

	echo -e "Compilation de "$project_dir" \c"

	mvn -f $project_dir clean install > /dev/null 2>&1
	mvn -f $project_dir compile > /dev/null 2>&1

	if [ "$generate_jar" = true ]; then
		mvn -f $project_dir exec:java > /dev/null 2>&1
	fi

	if [ $? = 0 ]; then
		echo "ok"
	else
		echo "echec"
	fi
}

# Respecter l'ordre de compilation : resources -> contexts -> (client ou server)
compile $ressources_dir false
compile $contexts_dir false
compile $client_dir true
compile $server_dir true

rm -r $output_dir 2> /dev/null
mkdir -p $output_dir

# Copie des jars générés dans le dossier output.
cp $client_dir"target/"$client_jarfile $output_dir
cp $server_dir"target/"$server_jarfile $output_dir

if [ $? = 0 ]; then
	echo "Fichiers générés dans "$output_dir
else
	echo "Echec de la génération des fichiers"
fi