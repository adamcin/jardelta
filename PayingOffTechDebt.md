# Paying Off Tech Debt

## The Tech Debt addressed by Jardiff

Jardiff aims to make it easier for developers to maintain AEM projects that consist primarily of OSGi bundles and 
FileVault content-packages, along with other standard Maven attached artifacts like javadoc and sources, which are also 
packaged as Jar archives. Jardiff aims to quantify the variations between two versions of the same artifact, to provide 
a reliable point of reference for changes to the tooling and CI processes that produce the artifact.


## The Prototype

```bash
#!/bin/bash
# usage: jar -tf oldbundle.jar | grep '^OSGI-INF/.*\.xml$' | xargs -L 1 ./compare.sh oldbundle.jar newbundle.jar
oldbundle="$1"
newbundle="$2"
comparepath="$3"

oldname="$(basename "$oldbundle")"
newname="$(basename "$newbundle")"
isxml=false
if [[ "$comparepath" == *".xml" ]]; then
    isxml=true
fi
echo "--- BEGIN ${oldname} ${comparepath}"
if [[ "$isxml" == true ]]; then
    unzip -q -c "$oldbundle" "$comparepath" | xmlstarlet fo -s 2
else
    unzip -q -c "$oldbundle" "$comparepath"
fi
echo "--- END   ${oldname} ${comparepath}"
echo "--- BEGIN ${newname} ${comparepath}"
if [[ "$isxml" == true ]]; then
    unzip -q -c "$newbundle" "$comparepath" | xmlstarlet fo -s 2
else
    unzip -q -c "$newbundle" "$comparepath"
fi
echo "--- END   ${newname} ${comparepath}"
echo
```


