package com.vltv.play

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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

        // Versão do app
        tvVersion.text = "Versão ${BuildConfig.VERSION_NAME}"

        // Controle parental
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

        // Limpar cache (Glide + temporários simples)
        cardClearCache.setOnClickListener {
            Thread {
                Glide.get(this).clearDiskCache()
            }.start()
            Glide.get(this).clearMemory()
            Toast.makeText(this, "Cache limpo", Toast.LENGTH_SHORT).show()
        }

        // Sobre (pode adicionar mais info depois)
        cardAbout.setOnClickListener {
            Toast.makeText(
                this,
                "VLTV PLAY\nVersão ${BuildConfig.VERSION_NAME}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
