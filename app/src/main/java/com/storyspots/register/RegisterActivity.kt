package com.storyspots.register

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.storyspots.MainActivity
import com.storyspots.R
import com.storyspots.login.LoginActivity
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import com.google.firebase.firestore.core.View

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerBtn: Button
    private lateinit var hintContainer: LinearLayout
    private lateinit var hintLength: TextView
    private lateinit var hintUpper: TextView
    private lateinit var hintSpecial: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        registerBtn = findViewById(R.id.btn_register)
        hintContainer = findViewById(R.id.password_hint_container)
        hintLength = findViewById(R.id.hint_length)
        hintUpper = findViewById(R.id.hint_upper)
        hintSpecial = findViewById(R.id.hint_special)

        password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val pwd = s.toString()

                // Show the hint container only when typing starts
                if (pwd.isNotEmpty()) {
                    hintContainer.visibility = android.view.View.VISIBLE
                } else {
                    hintContainer.visibility = android.view.View.GONE
                }

                // Update each hint color individually
                val lengthOk = pwd.length >= 6
                val upperOk = pwd.any { it.isUpperCase() }
                val specialOk = pwd.any { "!@#\$%^&*()_+=<>?/".contains(it) }

                val okColor = getColor(R.color.success_green)
                val errorColor = getColor(R.color.error_colour)

                hintLength.setTextColor(if (lengthOk) okColor else errorColor)
                hintUpper.setTextColor(if (upperOk) okColor else errorColor)
                hintSpecial.setTextColor(if (specialOk) okColor else errorColor)
            }
        })


        registerBtn.setOnClickListener {
            val emailText = email.text.toString()
            val passwordText = password.text.toString()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordStrong(passwordText)) {
                Toast.makeText(
                    this,
                    "Password must be at least 6 characters, include one uppercase letter and one special character.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            registerUser(emailText, passwordText)
        }
        val loginText = findViewById<TextView>(R.id.login_redirect)

        loginText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val exception = task.exception
                if (exception is FirebaseAuthUserCollisionException) {
                    Toast.makeText(this, "Email already in use", Toast.LENGTH_LONG).show()
                } else {
                    val error = exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Registration Failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val passwordPattern = "^(?=.*[A-Z])(?=.*[!@#\$%^&*()_+=<>?]).{6,}$"
        return password.matches(Regex(passwordPattern))
    }
}
