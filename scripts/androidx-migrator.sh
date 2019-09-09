#! /usr/bin/env bash

# inspired from https://gist.github.com/dudeinthemirror/cb4942e0ee5c3df0fcb678d1798e1d4d

SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPTS_DIR}"/.. && pwd)"
MAPPING_FILE="$SCRIPTS_DIR/androidx_class_map.csv"

replace=""

while IFS=, read -r from to
do
  replace+="s/$from/$to/g;"
done <<< "$(cat $MAPPING_FILE)"

echo $replace > replace.sed
i=0
(find $PROJECT_DIR \( -name "*.kt" -o -name "*.java" -o -name "*.xml" \) -type f ! -path '*/\.git*' ! -path '**/android/app/build/*' ! -path '**/\.idea/*' 2>/dev/null |
while read file
do
  grep -E "android.arch|android.databinding|android.support" "$file" > /dev/null 2>/dev/null
  ret=$?
  if (( ! ret )); then
    sed -i.bak -f replace.sed $file
    cmp --silent "$file" "$file.bak"
    ret=$?
    if (( ret ));then
      printf "\nDoing file %s\n" "$file"
     else
      i=$((i+1))
      printf '\r%2d skipped' $i
      rm -f "$file.bak"
    fi
  fi
done
echo
)

rm -rf replace.sed
exit