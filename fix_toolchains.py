import re

with open('be/build.gradle', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('JavaLanguageVersion.of(17)', 'JavaLanguageVersion.of(21)')

with open('be/build.gradle', 'w', encoding='utf-8') as f:
    f.write(content)

with open('.github/workflows/ci.yml', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("java-version: '17'", "java-version: '21'")

with open('.github/workflows/ci.yml', 'w', encoding='utf-8') as f:
    f.write(content)
