package com.vltv.play

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.vltv.play.BuildConfig

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchParental: Switch = findViewById(R.id.switchParental)
        val etPin: EditText = findViewById(R.id.etPin)
        val btnSavePin: Button = findViewById(R.id.btnSavePin)
        val cardClearCache: LinearLayout = findViewById(R.id.cardClearCache)
        val cardAbout: LinearLayout = findViewById(R.id.cardAbout)
        val tvVersion: TextView = findViewById(R.id.tvVersion)
        val cardLogout: LinearLayout = findViewById(R.id.cardLogout)

        // Versão do app via BuildConfig
        tvVersion.text = "Versão ${BuildConfig.VERSION_NAME}"

        // -------- CONTROLE PARENTAL --------
        switchParental.isChecked = ParentalControlManager.isEnabled(this)
        etPin.setText(ParentalControlManager.getPin(this))

        switchParental.setOnCheckedChangeListener { _, isChecked ->
            ParentalControlManager.setEnabled(this, isChecked)
        }

        btnSavePin.setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (pin.length != 4) {
                Toast.makeText(this, "PIN precisa ter 4 dígitos", Toast.LENGTH_SHORT).show()
            } else {
                ParentalControlManager.setPin(this, pin)
                Toast.makeText(this, "PIN salvo", Toast.LENGTH_SHORT).show()
            }
        }

        // -------- LIMPAR CACHE --------
        cardClearCache.setOnClickListener {
            Thread {
                Glide.get(this).clearDiskCache()
            }.start()
            Glide.get(this).clearMemory()
            Toast.makeText(this, "Cache limpo", Toast.LENGTH_SHORT).show()
        }

        // -------- SOBRE O APLICATIVO --------
        cardAbout.setOnClickListener {
            val msg = "VLTV PLAY\nVersão ${BuildConfig.VERSION_NAME}\nDesenvolvido por VLTV."
            AlertDialog.Builder(this)
                .setTitle("Sobre o aplicativo")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
        }

        // -------- SAIR DA CONTA --------
        cardLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sair")
                .setMessage("Deseja realmente sair e desconectar?")
                .setPositiveButton("Sim") { _, _ ->
                    val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }
}
