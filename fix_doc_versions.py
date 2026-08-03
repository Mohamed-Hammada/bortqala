import re

with open('README.md', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Java 26', 'Java 21')
content = content.replace('Node 24', 'Node 22')

with open('README.md', 'w', encoding='utf-8') as f:
    f.write(content)

with open('be/skills/hr-backend/SKILL.md', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Java 26', 'Java 21')

with open('be/skills/hr-backend/SKILL.md', 'w', encoding='utf-8') as f:
    f.write(content)
