const fs = require('fs');
const files = [
  'app/src/main/java/com/example/ui/AutoResumeScreens.kt',
  'app/src/main/java/com/example/ui/AutoResumeSecondaryFlows.kt',
  'app/src/main/java/com/example/ui/AutoResumeEditorWorkspace.kt',
  'app/src/main/java/com/example/ui/AutoResumeMainScreens.kt',
  'app/src/main/java/com/example/ui/MainViewModel.kt',
  'app/src/main/java/com/example/MainActivity.kt'
];

for (const file of files) {
  if (!fs.existsSync(file)) continue;
  let content = fs.readFileSync(file, 'utf8');
  if (!content.includes('import com.example.ui.locale.Text')) {
      content = content.replace('import com.example.ui.locale.tr', 'import com.example.ui.locale.tr\nimport com.example.ui.locale.Text');
      fs.writeFileSync(file, content, 'utf8');
  }
}
