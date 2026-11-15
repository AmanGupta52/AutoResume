package com.example.ui.locale

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

object LocaleManager {
    var currentLang by mutableStateOf("English")

    val dictionary = mapOf(
        "Hindi" to mapOf(
            "AutoResume" to "ऑटोरिज़्यूमे",
            "Create Professional Resumes Instantly" to "तुरंत पेशेवर रिज़्यूमे बनाएं",
            "Get Started" to "शुरू करें",
            "Build Professional Resumes" to "पेशेवर रिज़्यूमे बनाएं",
            "Choose from or generate selected modern templates optimized for applicant tracking systems (ATS)." to "चुने हुए आधुनिक टेम्प्लेट में से चुनें या जनरेट करें।",
            "Choose Premium Templates" to "प्रीमियम टेम्प्लेट चुनें",
            "Generate robust professional summaries and experience Work records optimized with metrics automatically." to "मजबूत पेशेवर सारांश और अनुभव कार्य रिकॉर्ड उत्पन्न करें।",
            "Download & Share Anywhere" to "डाउनलोड करें और कहीं भी शेयर करें",
            "Download pristine, modern PDFs with crisp formatting or share portable resume links instantly." to "स्पष्ट स्वरूपण के साथ पीडीएफ डाउनलोड करें या लिंक साझा करें।",
            "Skip" to "छोड़ें",
            "Next" to "अगला",
            "Finish & Start" to "समाप्त और शुरू करें",
            "Welcome Back" to "वापसी पर स्वागत है",
            "Sign in to manage your professional assets" to "साइन इन करें",
            "Email Address" to "ईमेल",
            "Password" to "पासवर्ड",
            "Biometric Unlock" to "बायोमेट्रिक আনলক",
            "Forgot Password?" to "पासवर्ड भूल गए?",
            "Sign In" to "साइन इन करें",
            "Or connect with" to "या इससे कनेक्ट करें",
            "Google" to "गूगल",
            "LinkedIn" to "लिंक्डइन",
            "Don't have an account?" to "कोई खाता नहीं है?",
            "Register" to "रजिस्टर",
            "Create Account" to "खाता बनाएं",
            "Join over 10,000+ job search builders" to "10,000+ से अधिक नौकरी खोजकर्ताओं से जुड़ें",
            "Full Name" to "पूरा नाम",
            "Phone Number" to "फ़ोन नंबर",
            "Confirm Password" to "पासवर्ड की पुष्टि करें",
            "I accept terms & safety guidelines." to "मैं शर्तों और सुरक्षा दिशानिर्देशों को स्वीकार करता हूँ।",
            "Register Account" to "खाता रजिस्टर करें",
            "Already registered?" to "पहले से पंजीकृत?",
            "Home" to "होम",
            "Resumes" to "रिज़्यूमे",
            "Templates" to "टेम्प्लेट",
            "Profile" to "प्रोफाइल",
            "Settings" to "सेटिंग्स",
            "Save" to "सहेजें",
            "Cancel" to "रद्द करें",
            "Edit" to "संपादित करें",
            "Delete" to "हटाएं",
            "Download PDF" to "पीडीएफ डाउनलोड करें",
            "Share" to "शेयर करें",
            "Print" to "प्रिंट करें"
        ),
        "Spanish" to mapOf(
            "AutoResume" to "AutoResume",
            "Create Professional Resumes Instantly" to "Crea Currículums Profesionales al Instante",
            "Get Started" to "Comenzar",
            "Build Professional Resumes" to "Construye Currículums Profesionales",
            "Choose Premium Templates" to "Elige Plantillas Premium",
            "Download & Share Anywhere" to "Descargar y Compartir en Cualquier Lugar",
            "Skip" to "Omitir",
            "Next" to "Siguiente",
            "Finish & Start" to "Finalizar y Comenzar",
            "Welcome Back" to "Bienvenido de nuevo",
            "Sign in to manage your professional assets" to "Inicia sesión para gestionar tus activos",
            "Email Address" to "Correo Electrónico",
            "Password" to "Contraseña",
            "Biometric Unlock" to "Desbloqueo Biométrico",
            "Forgot Password?" to "¿Olvidaste tu contraseña?",
            "Sign In" to "Iniciar Sesión",
            "Or connect with" to "O conéctate con",
            "Google" to "Google",
            "LinkedIn" to "LinkedIn",
            "Don't have an account?" to "¿No tienes una cuenta?",
            "Register" to "Registrarse",
            "Create Account" to "Crear Cuenta",
            "Full Name" to "Nombre Completo",
            "Phone Number" to "Número de Teléfono",
            "Confirm Password" to "Confirmar Contraseña",
            "I accept terms & safety guidelines." to "Acepto los términos y normativas.",
            "Register Account" to "Registrar Cuenta",
            "Already registered?" to "¿Ya estás registrado?",
            "Home" to "Inicio",
            "Resumes" to "Currículums",
            "Templates" to "Plantillas",
            "Profile" to "Perfil",
            "Settings" to "Ajustes",
            "Save" to "Guardar",
            "Cancel" to "Cancelar",
            "Edit" to "Editar",
            "Delete" to "Eliminar",
            "Download PDF" to "Descargar PDF",
            "Share" to "Compartir",
            "Print" to "Imprimir"
        ),
        "French" to mapOf(
            "Get Started" to "Commencer",
            "Skip" to "Passer",
            "Next" to "Suivant",
            "Sign In" to "Se connecter",
            "Home" to "Accueil",
            "Resumes" to "CV",
            "Profile" to "Profil",
            "Settings" to "Paramètres"
        ),
        "German" to mapOf(
            "Get Started" to "Anfangen",
            "Skip" to "Überspringen",
            "Next" to "Nächste",
            "Sign In" to "Anmelden",
            "Home" to "Startseite",
            "Resumes" to "Lebensläufe",
            "Profile" to "Profil",
            "Settings" to "Einstellungen"
        ),
        "Marathi" to mapOf(
            "Get Started" to "सुरू करा",
            "Home" to "मुख्यपृष्ठ",
            "Resumes" to "रिझ्युमे",
            "Profile" to "प्रोफाइल",
            "Settings" to "सेटिंग्ज"
        )
    )

    fun translate(text: String): String {
        return dictionary[currentLang]?.get(text) ?: text
    }
}

fun String.tr(): String {
    return LocaleManager.translate(this)
}

// Shadow Text composable
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    androidx.compose.material3.Text(
        text = LocaleManager.translate(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
