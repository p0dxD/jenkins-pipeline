#!/bin/bash

 hide="$1"
 while read FILE_NAME;
 do
	while read LINE; 
 	do 
		variable=$(echo "$LINE" | cut -f2 -d" " | cut -f1 -d"=")
		echo "Replacing: $variable"
		value=$(echo "$LINE" | cut -f2 -d" " | cut -f2 -d"="| tr -d "'")
		echo "Value: $value"
 		if [ ! -z hide ]; then
			sed -i -- "s/{{$variable}}/$value/g" $FILE_NAME
		else
			sed -i -- "s/$value/{{$variable}}/g" $FILE_NAME
		fi
	done < /home/.secrets
 done < <(find . -not \( -path ./.git -prune \) -type f -follow -print | grep -v "dockerfiles")

 exit 1
 