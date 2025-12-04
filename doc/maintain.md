```
git logshort | grep v2.12.0 -B99999999 | cut -d " " -f2- | grep -v "Yole v" | grep -i -v translat  | sed 's@^@- @'
```
