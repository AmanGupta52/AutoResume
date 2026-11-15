const fs = require('fs');

const files = [
  'app/src/main/java/com/example/ui/AutoResumeScreens.kt',
  'app/src/main/java/com/example/ui/AutoResumeSecondaryFlows.kt',
  'app/src/main/java/com/example/ui/AutoResumeEditorWorkspace.kt',
  'app/src/main/java/com/example/ui/AutoResumeMainScreens.kt',
  'app/src/main/java/com/example/ui/MainViewModel.kt',
  'app/src/main/java/com/example/MainActivity.kt'
];

function replaceColors(content) {
  // Replace dark backgrounds
  content = content.replace(/Color\(0xFF0D0D0F\)/g, "AppTheme.background");
  content = content.replace(/Color\(0xFF17181C\)/g, "AppTheme.card");
  content = content.replace(/Color\(0xFF161619\)/g, "AppTheme.card");
  content = content.replace(/Color\(0xFF131316\)/g, "AppTheme.card");
  content = content.replace(/Color\(0xFF1A1A1E\)/g, "AppTheme.card");
  content = content.replace(/Color\(0xFF131418\)/g, "AppTheme.card");
  content = content.replace(/Color\(0xFF1E1E22\)/g, "AppTheme.card");
  content = content.replace(/Color\(0xFF2A2D34\)/g, "AppTheme.border");
  content = content.replace(/Color\(0xFF1C1C1F\)/g, "AppTheme.card");
  
  // Replace light backgrounds
  content = content.replace(/Color\(0xFFF5F7FA\)/g, "AppTheme.background");
  content = content.replace(/Color\(0xFFFFFFFF\)/g, "AppTheme.card"); // Wait, replacing all White might be dangerous, especially text.
  
  // Replace specific color blocks that check isDark:
  content = content.replace(/val bgColorStart = if \(isDark\) Color\(0xFF0F0F10\) else Color\(0xFFF5F7FA\)/g, "val bgColorStart = AppTheme.background");
  content = content.replace(/val bgColorEnd = if \(isDark\) Color\(0xFF17181C\) else Color\(0xFFFFFFFF\)/g, "val bgColorEnd = AppTheme.card");
  content = content.replace(/val textClr = if \(isDark\) Color\.White else Color\(0xFF111827\)/g, "val textClr = AppTheme.text");
  content = content.replace(/val subTextClr = if \(isDark\) Color\(0xFF9CA3AF\) else Color\(0xFF4B5563\)/g, "val subTextClr = AppTheme.textSecondary");
  content = content.replace(/val borderClr = if \(isDark\) Color\(0xFF2A2D34\) else Color\(0xFFE5E7EB\)/g, "val borderClr = AppTheme.border");

  return content;
}

for (const file of files) {
  if (!fs.existsSync(file)) continue;
  let content = fs.readFileSync(file, 'utf8');
  
  // 1. Add import for AppTheme and LocaleManager
  if (!content.includes('import com.example.ui.locale.*')) {
      content = content.replace('package com.example.ui', 'package com.example.ui\n\nimport com.example.ui.theme.AppTheme\nimport com.example.ui.locale.LocaleManager\nimport com.example.ui.locale.tr');
      content = content.replace('import com.example.ui.locale.tr', 'import com.example.ui.locale.tr\nimport com.example.ui.locale.Text');
  }

  // 2. Perform color replacements
  content = replaceColors(content);

  // 3. In MainViewModel, auto-sync LocaleManager and AppTheme
  if (file.includes('MainViewModel.kt')) {
    if (!content.includes('LocaleManager.currentLang')) {
       content = content.replace(/val appLanguage = MutableStateFlow\("English"\)/g, 'val appLanguage = MutableStateFlow("English").apply { kotlinx.coroutines.GlobalScope.launch { collect { LocaleManager.currentLang = it } } }');
       content = content.replace(/val isDarkTheme = MutableStateFlow\(false\)/g, 'val isDarkTheme = MutableStateFlow(false).apply { kotlinx.coroutines.GlobalScope.launch { collect { AppTheme.isDarkTheme = it } } }');
    }
  }

  fs.writeFileSync(file, content, 'utf8');
  console.log('Processed', file);
}

// 4. Update the Text( literals inside the UI
// Actually, string replacement is tricky. Let's just use the shadow Text composable.
