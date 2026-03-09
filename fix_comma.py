path = r"C:\Users\Yahya H\Desktop\dompetku2\src\App.jsx"
d = open(path, encoding="utf-8").read()

# Fix trailing comma left by previous patch
old = '\n  , "Format yang Didukung"'
new = '\n   "Format yang Didukung"'
if old in d:
    d = d.replace(old, new, 1)
    print("✓ Fixed comma before Format yang Didukung")
else:
    print("~ Not found, checking line 84...")
    lines = d.split("\n")
    for i, l in enumerate(lines):
        if l.strip().startswith(', "Format'):
            lines[i] = l.replace(', "Format', '"Format')
            print(f"✓ Fixed line {i+1}")
            break
    d = "\n".join(lines)

open(path, "w", encoding="utf-8").write(d)
print("Done")
