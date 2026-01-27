# DSV File Viewer - IntelliJ IDEA Plugin

A beautiful, interactive table viewer for delimiter-separated values (DSV) files in IntelliJ IDEA.

## Features

- **Multiple Delimiter Support**: CSV, TSV, and custom delimiters (semicolon, pipe, space, etc.)
- **Interactive Table View**: Sort columns, resize, search, and copy cells
- **Smart Parsing**: Handles quoted fields, escaped quotes, and multi-line content
- **Auto-Detection**: Automatically detects delimiter based on file extension and content
- **Header Toggle**: Switch between using first row as headers or auto-generated column names
- **Export**: Export current view to CSV
- **Dual View**: Toggle between table view and raw text editor
- **Theme Support**: Adapts to IntelliJ's light and dark themes

## Installation

### From JetBrains Marketplace
1. Go to **Settings/Preferences → Plugins → Marketplace**
2. Search for "DSV File Viewer"
3. Click **Install**

### Manual Installation
1. Download the plugin JAR from releases
2. Go to **Settings/Preferences → Plugins**
3. Click ⚙️ → **Install Plugin from Disk...**
4. Select the downloaded JAR file

## Usage

1. Open any `.csv`, `.tsv`, or `.dsv` file in IntelliJ IDEA
2. The file will automatically open in the table view
3. Use the toolbar to:
   - Change delimiter
   - Toggle header row interpretation
   - Search within the table
   - Export to CSV
   - Switch to text view

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl/Cmd + C` | Copy selected cells |
| `Ctrl/Cmd + A` | Select all |
| `Ctrl/Cmd + F` | Focus search field |
| Click header | Sort by column |

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourcompany/dsv-viewer-idea-plugin.git
cd dsv-viewer-idea-plugin

# Build the plugin
./gradlew build

# Run in development IDE
./gradlew runIde

# Run tests
./gradlew test
```

## Requirements

- IntelliJ IDEA 2023.3 or later
- Java 17 or later

## License

MIT License - see [LICENSE](LICENSE) for details.
