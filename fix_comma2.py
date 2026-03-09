path = r"C:\Users\Yahya H\Desktop\dompetku2\src\App.jsx"
lines = open(path, encoding="utf-8").read().split("\n")
fixed = 0
for i, l in enumerate(lines):
    stripped = l.strip()
    if stripped == "," or stripped == ",":
        lines[i] = ""
        print(f"✓ Removed orphan comma at line {i+1}")
        fixed += 1
open(path, "w", encoding="utf-8").write("\n".join(lines))
print(f"Done. Fixed {fixed} lines.")
