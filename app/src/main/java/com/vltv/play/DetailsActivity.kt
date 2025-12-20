package com.vltv.play

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailsActivity : AppCompatActivity() {

    private lateinit var tvPlot: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvCast: TextView
    private lateinit var tvDirector: TextView
    private lateinit var tvRating: TextView
    private lateinit var imgPoster: ImageView
    private lateinit var btnPlay: Button
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnResume: Button

    private var streamId: Int = 0
    private var extension: String = "mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val name = intent.getStringExtra("title") ?: "Sem Título"
        val icon = intent.getStringExtra("icon")
        streamId = intent.getIntExtra("stream_id", 0)
        extension = intent.getStringExtra("extension") ?: "mp4"

        tvPlot = findViewById(R.id.tvPlot)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvGenre = findViewById(R.id.tvGenre)
        tvCast = findViewById(R.id.tvCast)
        tvDirector = findViewById(R.id.tvDirector)
        tvRating = findViewById(R.id.tvRating)
        imgPoster = findViewById(R.id.imgPoster)
        btnPlay = findViewById(R.id.btnPlay)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnResume = findViewById(R.id.btnResume)

        tvTitle.text = name

        Glide.with(this)
            .load(icon)
            .placeholder(R.mipmap.ic_launcher)
            .into(imgPoster)

        // -------- FAVORITOS --------
        val isFavInicial = getFavMovies(this).contains(streamId)
        atualizarIconeFavorito(isFavInicial)

        btnFavorite.setOnClickListener {
            val favs = getFavMovies(this)
            val novoFav: Boolean
            if (favs.contains(streamId)) {
                favs.remove(streamId)
                novoFav = false
            } else {
                favs.add(streamId)
                novoFav = true
            }
            saveFavMovies(this, favs)
            atualizarIconeFavorito(novoFav)
        }

        // -------- CONTINUAR ASSISTINDO --------
        configurarBotaoResume()

        btnPlay.setOnClickListener {
            abrirPlayer(name, startPositionMs = 0L)
        }
        btnPlay.requestFocus()

        btnResume.setOnClickListener {
            val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
            val keyBase = "movie_resume_$streamId"
            val pos = prefs.getLong("${keyBase}_pos", 0L)
            abrirPlayer(name, startPositionMs = pos)
        }

        carregarDetalhes(streamId)
    }

    private fun abrirPlayer(name: String, startPositionMs: Long) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("stream_id", streamId)
        intent.putExtra("stream_ext", extension)
        intent.putExtra("stream_type", "movie")
        intent.putExtra("channel_name", name)   // nome do filme para o player

        if (startPositionMs > 0L) {
            intent.putExtra("start_position_ms", startPositionMs)
        }
        startActivity(intent)
    }

    private fun configurarBotaoResume() {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val keyBase = "movie_resume_$streamId"
        val pos = prefs.getLong("${keyBase}_pos", 0L)
        val dur = prefs.getLong("${keyBase}_dur", 0L)

        val existe = pos > 30_000L && dur > 0L && pos < (dur * 0.95).toLong()
        btnResume.visibility = if (existe) Button.VISIBLE else Button.GONE
    }

    // ------------ FAVORITOS ------------

    private fun getFavMovies(context: Context): MutableSet<Int> {
        val prefs = context.getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("fav_movies", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    private fun saveFavMovies(context: Context, ids: Set<Int>) {
        val prefs = context.getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("fav_movies", ids.map { it.toString() }.toSet())
            .apply()
    }

    private fun atualizarIconeFavorito(isFav: Boolean) {
        val res = if (isFav) R.drawable.ic_star_filled else R.drawable.ic_star_border
        btnFavorite.setImageResource(res)
    }

    // ------------ DETALHES VOD ------------

    private fun carregarDetalhes(streamId: Int) {
        val prefs = getSharedPreferences("vltv_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "") ?: ""
        val password = prefs.getString("password", "") ?: ""

        XtreamApi.service.getVodInfo(username, password, vodId = streamId)
            .enqueue(object : Callback<VodInfoResponse> {
                override fun onResponse(
                    call: Call<VodInfoResponse>,
                    response: Response<VodInfoResponse>
                ) {
                    if (response.isSuccessful && response.body()?.info != null) {
                        val info = response.body()!!.info!!

                        tvPlot.text = info.plot ?: "Sinopse indisponível."
                        tvGenre.text = "Gênero: ${info.genre ?: "N/A"}"
                        tvCast.text = "Elenco: ${info.cast ?: "N/A"}"
                        tvRating.text = "Nota: ${info.rating ?: "N/A"}"
                        tvDirector.text = "Diretor: ${info.director ?: "N/A"}"

                        if (!info.movie_image.isNullOrEmpty()) {
                            Glide.with(this@DetailsActivity)
                                .load(info.movie_image)
                                .into(imgPoster)
                        }
                    } else {
                        tvPlot.text = "Não foi possível carregar detalhes."
                    }
                }

                override fun onFailure(call: Call<VodInfoResponse>, t: Throwable) {
                    tvPlot.text = "Erro de conexão ao buscar detalhes."
                }
            })
    }
}
